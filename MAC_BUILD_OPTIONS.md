# macOS Build Options

There are two ways to build the macOS application, depending on your needs.

## Option 1: Simple Build (Recommended)

**Script:** `./build-mac-app-simple.sh`

### Pros
✅ **Works reliably** - Uses system Java, no bundled runtime  
✅ **Smaller size** - ~109MB DMG (vs ~350MB with bundled Java)  
✅ **Faster build** - No jpackage complexity  
✅ **Better for servers** - Designed for Spring Boot applications  
✅ **Easier debugging** - Clear logs and error messages  

### Cons
❌ **Requires Java** - Users must have Java 21+ installed  
❌ **Not self-contained** - Depends on system Java  

### When to Use
- Internal distribution where Java is already installed
- Development and testing
- When you need smaller file sizes
- When you want simpler troubleshooting

### Build Command
```bash
./build-mac-app-simple.sh
```

**Output:** `target/mac-app/DocMgmt-1.0.0.dmg` (~109MB)

## Option 2: jpackage Build (Self-Contained)

**Script:** `./build-mac-dmg.sh`

### Pros
✅ **Self-contained** - Bundles Java runtime  
✅ **No dependencies** - Users don't need Java installed  
✅ **Professional** - Standard macOS app packaging  
✅ **Better integration** - Full macOS app bundle features  

### Cons
❌ **Larger size** - ~350MB DMG (includes Java runtime)  
❌ **Slower build** - jpackage takes longer  
❌ **More complex** - Harder to debug launcher issues  
❌ **GUI-focused** - jpackage designed for GUI apps, not servers  

### Known Issue
The jpackage build may have issues with server applications that run in the background. The app launches but the Java process exits immediately. This is because jpackage's default launcher is designed for GUI applications that keep the main thread alive, not background servers.

### When to Use
- Public distribution to non-technical users
- When users don't have Java installed
- When self-containment is critical
- After fixing the launcher issue (see below)

### Build Command
```bash
./build-mac-dmg.sh
```

**Output:** `target/dist/DocMgmt-1.0.0.dmg` (~350MB)

## Comparison Table

| Feature | Simple Build | jpackage Build |
|---------|-------------|----------------|
| **Size** | ~109MB | ~350MB |
| **Java Required** | Yes (21+) | No (bundled) |
| **Build Time** | Fast (~30s) | Slower (~3min) |
| **Works Now** | ✅ Yes | ⚠️ Needs fix |
| **Self-Contained** | No | Yes |
| **Debugging** | Easy | Complex |
| **Use Case** | Internal/Dev | Public/Distribution |

## Current Recommendation

**Use the Simple Build** (`./build-mac-app-simple.sh`) because:
1. It works reliably right now
2. The Spring Boot server starts correctly
3. Logs are clear and accessible
4. File size is reasonable
5. Most developers have Java installed

The jpackage build needs the launcher issue fixed before it's production-ready for server applications.

## Testing the Application

After building with either method:

```bash
# For simple build
open target/mac-app/DocMgmt-1.0.0.dmg

# For jpackage build  
open target/dist/DocMgmt-1.0.0.dmg
```

Then:
1. Drag DocMgmt.app to Applications
2. Double-click DocMgmt in Applications
3. Wait ~8 seconds for server to start
4. Browser should open to http://localhost:8082/docmgmt

## Troubleshooting

### Check if Server is Running
```bash
ps aux | grep document-management | grep -v grep
```

### Check Port
```bash
lsof -i :8082
```

### View Startup Logs
```bash
cat ~/Library/Application\ Support/DocMgmt/logs/startup.log
```

### View Application Logs
```bash
tail -f ~/Library/Application\ Support/DocMgmt/logs/application.log
```

### Stop the Server
```bash
# Find PID
ps aux | grep document-management | grep -v grep

# Kill process
kill <PID>
```

## Fixing the jpackage Launcher

To make the jpackage build work properly for server applications, you would need to:

1. Create a custom launcher that:
   - Starts the Java server in the background
   - Keeps the main process alive
   - Monitors the server process
   - Handles shutdown properly

2. Or use a Java GUI wrapper that:
   - Shows a menu bar icon
   - Displays server status
   - Provides start/stop controls
   - Opens browser automatically

This is beyond the scope of the initial build setup but could be added later if self-contained distribution is required.

## Installation Instructions

### For Simple Build Users

**Prerequisites:** Java 21 or later must be installed

```bash
# Check Java version
java -version

# If needed, install Java
brew install openjdk@21
```

**Installation:**
1. Download `DocMgmt-1.0.0.dmg`
2. Double-click to open
3. Drag DocMgmt to Applications
4. Double-click to run

### For jpackage Build Users

**Prerequisites:** None (Java is bundled)

**Installation:**
1. Download `DocMgmt-1.0.0.dmg`
2. Double-click to open
3. Drag DocMgmt to Applications
4. Double-click to run
5. If blocked: System Settings > Privacy & Security > "Open Anyway"

## Summary

- **Use `build-mac-app-simple.sh` for now** - it works reliably
- File size is reasonable at 109MB
- Users need Java 21+ installed
- Perfect for internal distribution and testing
- The jpackage build needs more work for server applications

Both builds create proper macOS applications with:
- ✅ Correct filesystem permissions
- ✅ Application Support directory usage
- ✅ Automatic browser launch
- ✅ Standard macOS app behavior
- ✅ Proper logging

The simple build is production-ready for environments where Java is available.
