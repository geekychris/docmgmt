# macOS Permissions and Security

This document explains how filesystem permissions and security work for the DocMgmt macOS application.

## Overview

macOS has strict security requirements for applications. This application is configured to properly request and use filesystem permissions while maintaining security best practices.

## Permissions Configured

### 1. Filesystem Access

The application is configured with the following filesystem permissions:

#### User-Selected Files (Read/Write)
- **What it does**: Allows reading and writing files that users explicitly select
- **Why needed**: For importing and exporting documents
- **Entitlement**: `com.apple.security.files.user-selected.read-write`

#### User Home Directory (Read)
- **What it does**: Allows reading files from user's home directory
- **Why needed**: For accessing the Application Support folder
- **Entitlement**: `com.apple.security.files.user-selected.read-only`

#### Application Support Directory
- **Location**: `~/Library/Application Support/DocMgmt/`
- **Automatic**: No permission prompt needed
- **Contents**:
  - `docmgmt_db.mv.db` - H2 database file
  - `lucene_index/` - Search index
  - `logs/` - Application logs

### 2. Network Access

#### Server
- **What it does**: Allows application to run a web server
- **Why needed**: For the web UI on port 8082
- **Entitlement**: `com.apple.security.network.server`

#### Client
- **What it does**: Allows outbound network connections
- **Why needed**: For potential external API calls
- **Entitlement**: `com.apple.security.network.client`

### 3. Java Runtime Permissions

#### JIT Compilation
- **What it does**: Allows Just-In-Time compilation
- **Why needed**: For Java runtime performance
- **Entitlement**: `com.apple.security.cs.allow-jit`

#### Unsigned Executable Memory
- **What it does**: Allows JVM to execute dynamically generated code
- **Why needed**: Required by Java Virtual Machine
- **Entitlement**: `com.apple.security.cs.allow-unsigned-executable-memory`

#### Library Validation
- **What it does**: Disables strict library validation
- **Why needed**: Allows JVM native libraries to load
- **Entitlement**: `com.apple.security.cs.disable-library-validation`

### 4. App Sandbox

The application is **NOT sandboxed** (`com.apple.security.app-sandbox = false`) to allow:
- Full filesystem access for document management
- Unrestricted network access for web server
- Access to user's documents and downloads folders

## Permission Prompts

When users first launch the application, macOS may show prompts for:

### 1. Security Warning (Unsigned App)
```
"DocMgmt" cannot be opened because the developer cannot be verified.
```

**Solution**:
1. Go to System Settings > Privacy & Security
2. Scroll down to see "DocMgmt was blocked"
3. Click "Open Anyway"

**Or via Terminal**:
```bash
xattr -dr com.apple.quarantine /Applications/DocMgmt.app
```

### 2. Folder Access Permissions

The app may request access to:
- **Documents folder**: For importing/exporting documents
- **Downloads folder**: For importing downloaded files
- **Desktop**: For quick access to files
- **External drives**: For document import

**These prompts appear automatically when needed.**

## Files Configured

### 1. `packaging/macos/entitlements.plist`

Defines security entitlements for the application:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "...">
<plist version="1.0">
<dict>
    <key>com.apple.security.cs.allow-jit</key>
    <true/>
    
    <key>com.apple.security.files.user-selected.read-write</key>
    <true/>
    
    <key>com.apple.security.network.server</key>
    <true/>
    
    <!-- ... more entitlements ... -->
</dict>
</plist>
```

### 2. `packaging/macos/Info.plist`

Defines permission descriptions shown to users:

```xml
<key>NSDocumentsFolderUsageDescription</key>
<string>DocMgmt needs access to read and write document files.</string>

<key>NSDownloadsFolderUsageDescription</key>
<string>DocMgmt needs access to import documents from your Downloads folder.</string>
```

These messages appear in permission prompts.

## JVM Options Configured

The build includes JVM options for proper filesystem access:

```bash
-Djava.io.tmpdir=$TMPDIR          # Use macOS temp directory
-Duser.home=$HOME                 # Preserve user home directory
-Xmx2048m                         # Max 2GB heap
-Xms512m                          # Initial 512MB heap
-Dserver.port=8082                # Web server port
-Dspring.profiles.active=production
```

## Testing Permissions

### Test Filesystem Access

```bash
# Launch the app
open /Applications/DocMgmt.app

# Check if data directory was created
ls -la ~/Library/Application\ Support/DocMgmt/

# Expected output:
# drwxr-xr-x  docmgmt_db.mv.db
# drwxr-xr-x  lucene_index/
# drwxr-xr-x  logs/
```

### Test File Upload

1. Open http://localhost:8082/docmgmt
2. Create a new document
3. Upload a file from:
   - Documents folder
   - Downloads folder
   - Desktop
   - External drive

If permission prompts appear, click "Allow" or "OK".

### Test Network Access

```bash
# Check if server is listening
lsof -i :8082

# Expected output:
# COMMAND  PID USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
# java    1234 user   123u IPv6 0x...  0t0  TCP *:8082 (LISTEN)
```

## Granting Additional Permissions

### Full Disk Access (Optional)

For unrestricted filesystem access:

1. Open **System Settings** > **Privacy & Security**
2. Click **Full Disk Access**
3. Click the **+** button
4. Navigate to `/Applications/DocMgmt.app`
5. Select and click **Open**
6. Toggle DocMgmt **ON**

**Note**: This is optional and not required for normal operation.

### Firewall Exception (If Needed)

If macOS Firewall blocks the web server:

1. Open **System Settings** > **Network** > **Firewall**
2. Click **Options**
3. Find DocMgmt in the list
4. Select **Allow incoming connections**

## Code Signing (For Distribution)

For wider distribution without security warnings, sign the application:

### 1. Get Developer Certificate

```bash
# Check available certificates
security find-identity -v -p codesigning

# Should show:
# 1) ABC123... "Developer ID Application: Your Name (TEAM_ID)"
```

### 2. Sign the Application

```bash
# Sign with entitlements
codesign --sign "Developer ID Application: Your Name" \
         --entitlements packaging/macos/entitlements.plist \
         --options runtime \
         --timestamp \
         --deep \
         target/dist/DocMgmt.app

# Verify signature
codesign --verify --deep --strict --verbose=2 target/dist/DocMgmt.app
```

### 3. Notarize with Apple

```bash
# Create DMG
hdiutil create -volname "DocMgmt" \
               -srcfolder target/dist/DocMgmt.app \
               -ov -format UDZO \
               target/dist/DocMgmt-1.0.0.dmg

# Submit for notarization
xcrun notarytool submit target/dist/DocMgmt-1.0.0.dmg \
                       --apple-id "your@email.com" \
                       --team-id "TEAM_ID" \
                       --password "app-specific-password" \
                       --wait

# Staple notarization ticket
xcrun stapler staple target/dist/DocMgmt-1.0.0.dmg
```

## Troubleshooting

### Problem: "App is damaged and can't be opened"

**Cause**: Gatekeeper quarantine flag

**Solution**:
```bash
xattr -dr com.apple.quarantine /Applications/DocMgmt.app
```

### Problem: "Permission denied" when writing files

**Cause**: Missing filesystem permissions

**Solution**:
1. Grant Full Disk Access (see above)
2. Or check ~/Library/Application Support/DocMgmt/ ownership:
```bash
ls -la ~/Library/Application\ Support/DocMgmt/
# Should be owned by your user
```

### Problem: Can't access Documents/Downloads folder

**Cause**: Permission not granted

**Solution**:
1. Go to System Settings > Privacy & Security
2. Click **Files and Folders**
3. Find DocMgmt and enable required folders
4. Or re-launch app and accept permission prompts

### Problem: Port 8082 access denied

**Cause**: Another process using the port

**Solution**:
```bash
# Find process
lsof -i :8082

# Kill process
kill -9 <PID>

# Or change port in build-mac-dmg.sh:
--java-options "-Dserver.port=8083"
```

### Problem: App won't start after macOS update

**Cause**: Security policies changed

**Solution**:
1. Remove and re-add Full Disk Access permission
2. Re-sign the application:
```bash
codesign --force --deep --sign - /Applications/DocMgmt.app
```

## Best Practices

### For Developers

1. **Test on clean Mac**: Always test on a Mac without developer tools
2. **Document permissions**: Keep this file updated with permission changes
3. **Code sign early**: Sign builds before distributing to testers
4. **Test permission prompts**: Verify all prompts show correct descriptions

### For Users

1. **Accept permission prompts**: Click "Allow" when prompted
2. **Check System Settings**: Verify permissions in Privacy & Security
3. **Keep macOS updated**: Latest macOS has best security support
4. **Use official DMG**: Only install from trusted sources

## Security Considerations

### What the App CAN Do

✅ Read/write to its own Application Support folder  
✅ Access files you explicitly select (open/save dialogs)  
✅ Run a web server on localhost  
✅ Access folders you grant permission to  

### What the App CANNOT Do

❌ Access system files without permission  
❌ Access other apps' data  
❌ Make changes outside its data directory (without permission)  
❌ Access camera/microphone  
❌ Access contacts/calendar  

### Data Storage

All application data is stored in standard macOS locations:

```
~/Library/Application Support/DocMgmt/
├── docmgmt_db.mv.db         # Database (user documents metadata)
├── docmgmt_db.trace.db      # Database trace log
├── lucene_index/            # Search index
└── logs/
    └── application.log      # Application logs
```

To **completely remove** all data:
```bash
rm -rf ~/Library/Application\ Support/DocMgmt/
rm -rf /Applications/DocMgmt.app
```

## Privacy

- **No telemetry**: Application does not send any data externally
- **Local only**: All data stored locally on your Mac
- **No cloud**: No cloud storage or synchronization
- **No tracking**: No analytics or usage tracking

## Summary

The DocMgmt application is configured with appropriate permissions for:
- ✅ Filesystem access for document management
- ✅ Network access for local web server
- ✅ Java runtime requirements
- ✅ Proper security entitlements
- ✅ Clear permission descriptions

All permissions follow macOS security best practices while providing full functionality for document management.

## Support

For permission-related issues:
1. Check this document first
2. See BUILD_MAC.md for build configuration
3. See QUICK_START_MAC.md for common issues

## References

- [Apple Code Signing Guide](https://developer.apple.com/documentation/security/code_signing_services)
- [Apple Notarization Guide](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution)
- [App Sandbox Entitlements](https://developer.apple.com/documentation/bundleresources/entitlements)
