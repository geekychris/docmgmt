# Building macOS DMG Installer

This guide explains how to build a native macOS application with DMG installer for the Document Management System.

## Overview

The build process creates a native macOS application (.app) bundled with a Java runtime and packaged as a DMG installer. Users can install the application by simply dragging it to their Applications folder - no Java installation required.

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 21 or later**
   - Must include `jpackage` tool (available in JDK 14+)
   - Verify installation:
     ```bash
     java -version
     jpackage --version
     ```
   - If not installed:
     - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/)
     - Or install via Homebrew: `brew install openjdk@21`

2. **Apache Maven 3.6 or later**
   - Verify installation:
     ```bash
     mvn -version
     ```
   - If not installed: `brew install maven`

3. **macOS Development Tools** (optional, for custom icons)
   - Xcode Command Line Tools: `xcode-select --install`
   - Or ImageMagick for icon creation: `brew install imagemagick`

### System Requirements

- macOS 10.14 (Mojave) or later
- At least 4GB of free disk space for build
- Internet connection for Maven dependencies

## Quick Start

### Build DMG with One Command

```bash
./build-mac-dmg.sh
```

This script will:
1. Check prerequisites
2. Clean previous builds
3. Build the application in production mode
4. Create a DMG installer with bundled Java runtime

The resulting DMG will be at: `target/dist/DocMgmt-1.0.0.dmg`

## Step-by-Step Build Process

### Step 1: Prepare Application Icon (Optional)

The application can use a custom icon for better branding.

#### Option A: Create Default Icon
```bash
cd packaging/macos
./create-default-icon.sh
```

This creates a simple blue icon with "DM" text.

#### Option B: Create Custom Icon

See `packaging/macos/CREATE_ICON.md` for detailed instructions.

Quick version:
1. Create a 1024x1024 PNG image
2. Convert to .icns format:
   ```bash
   # From a PNG file
   mkdir app-icon.iconset
   sips -z 16 16 icon.png --out app-icon.iconset/icon_16x16.png
   sips -z 32 32 icon.png --out app-icon.iconset/icon_16x16@2x.png
   # ... (see CREATE_ICON.md for all sizes)
   iconutil -c icns app-icon.iconset -o packaging/macos/app-icon.icns
   rm -rf app-icon.iconset
   ```

#### Option C: Skip Custom Icon

The build will work without an icon (uses default Java icon). The build script will prompt you to confirm.

### Step 2: Build Application

```bash
# Clean previous builds
mvn clean

# Build in production mode
mvn package -Pproduction -DskipTests
```

This compiles the application, builds the Vaadin frontend, and creates an executable JAR file.

**What happens:**
- Compiles Java source code
- Builds Vaadin UI components (production mode)
- Packages everything into `target/document-management-0.0.1-SNAPSHOT.jar`
- Build time: 2-5 minutes (first build may take longer)

### Step 3: Create DMG Installer

#### Automatic Method (Recommended)

```bash
./build-mac-dmg.sh
```

#### Manual Method

```bash
jpackage \
  --name DocMgmt \
  --app-version 1.0.0 \
  --vendor "DocMgmt Project" \
  --description "Document Management System with versioning and full-text search" \
  --type dmg \
  --input target \
  --main-jar document-management-0.0.1-SNAPSHOT.jar \
  --main-class com.docmgmt.DocumentManagementApplication \
  --dest target/dist \
  --java-options "-Xmx2048m" \
  --java-options "-Dserver.port=8082" \
  --java-options "-Dspring.profiles.active=production" \
  --mac-package-name DocMgmt \
  --mac-package-identifier com.docmgmt.app \
  --icon packaging/macos/app-icon.icns
```

**What happens:**
- Creates application bundle (DocMgmt.app)
- Bundles Java runtime (no separate Java installation needed)
- Creates DMG installer for distribution
- Includes application icon and metadata
- Build time: 3-5 minutes
- Final DMG size: ~250-350 MB (includes Java runtime)

## Build Output

After successful build, you'll find:

```
target/
├── dist/
│   └── DocMgmt-1.0.0.dmg          # Distributable DMG installer
└── document-management-0.0.1-SNAPSHOT.jar  # Executable JAR (if needed)
```

## Testing the Build

### Test DMG Installation

1. Open the DMG:
   ```bash
   open target/dist/DocMgmt-1.0.0.dmg
   ```

2. The DMG window will open - drag DocMgmt.app to Applications folder

3. Launch the application:
   - From Applications folder, double-click DocMgmt
   - Or from command line: `open /Applications/DocMgmt.app`

4. The application will:
   - Start a web server on port 8082
   - Automatically open your default browser to http://localhost:8082/docmgmt
   - Store data in `~/Library/Application Support/DocMgmt/`

### Verify Installation

Check that data is being stored correctly:
```bash
ls -la ~/Library/Application\ Support/DocMgmt/
# Should show:
# - docmgmt_db.mv.db (database)
# - lucene_index/ (search index)
# - logs/ (application logs)
```

### Test Application Features

1. **Web UI**: Navigate to http://localhost:8082/docmgmt
2. **Create Document**: Add a test document with file upload
3. **Search**: Verify full-text search works
4. **Persistence**: Restart app and verify data is retained

## Distribution

### Distributing the DMG

The DMG file (`target/dist/DocMgmt-1.0.0.dmg`) is ready for distribution:

1. **Direct Distribution**
   - Upload to file sharing service
   - Send via email (if size permits)
   - Host on website for download

2. **GitHub Release**
   ```bash
   # Tag release
   git tag v1.0.0
   git push origin v1.0.0
   
   # Create GitHub release and attach DMG file
   ```

3. **Internal Distribution**
   - Share via company network
   - Add to software distribution system

### Installation Instructions for End Users

Provide these instructions to users:

```markdown
## Installing DocMgmt

1. Download `DocMgmt-1.0.0.dmg`
2. Double-click the DMG file to open it
3. Drag the DocMgmt icon to the Applications folder
4. Eject the DMG (right-click and select "Eject")
5. Open Applications folder and double-click DocMgmt
6. If macOS shows a security warning:
   - Go to System Settings > Privacy & Security
   - Click "Open Anyway" next to the DocMgmt message
7. The application will start and open in your browser

## Using DocMgmt

- Web interface: http://localhost:8082/docmgmt
- Data location: ~/Library/Application Support/DocMgmt/
- To stop: Quit the application from the Dock or menu bar
- To uninstall: Delete DocMgmt.app from Applications
```

### Code Signing and Notarization (Optional)

For wider distribution, consider code signing:

1. **Get Apple Developer Account** ($99/year)

2. **Create Certificates**
   - Developer ID Application certificate
   - Developer ID Installer certificate

3. **Sign the application**
   ```bash
   codesign --sign "Developer ID Application: Your Name" \
            --timestamp \
            --options runtime \
            --entitlements entitlements.plist \
            --deep \
            target/dist/DocMgmt.app
   ```

4. **Notarize with Apple**
   ```bash
   xcrun notarytool submit target/dist/DocMgmt-1.0.0.dmg \
                          --apple-id "your@email.com" \
                          --team-id "TEAM_ID" \
                          --wait
   
   # Staple notarization ticket
   xcrun stapler staple target/dist/DocMgmt-1.0.0.dmg
   ```

**Note:** Code signing is optional for internal/testing distribution but required for public distribution to avoid security warnings.

## Troubleshooting

### Build Issues

#### Problem: "jpackage: command not found"

**Solution:** Install JDK 14 or later with jpackage support:
```bash
brew install openjdk@21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

#### Problem: Maven build fails with "Java version mismatch"

**Solution:** Ensure JAVA_HOME points to Java 21+:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn -version  # Verify Maven sees correct Java version
```

#### Problem: "Error: Invalid or corrupt jarfile"

**Solution:** Clean and rebuild:
```bash
mvn clean
rm -rf target/
./build-mac-dmg.sh
```

#### Problem: Vaadin build fails

**Solution:** Clear frontend build cache:
```bash
rm -rf node_modules/ package-lock.json
npm install
mvn clean package -Pproduction
```

### Runtime Issues

#### Problem: "The application can't be opened" security warning

**Solution:** 
- Go to System Settings > Privacy & Security
- Click "Open Anyway" button
- Or run: `xattr -dr com.apple.quarantine /Applications/DocMgmt.app`

#### Problem: Application starts but browser doesn't open

**Solution:** Manually navigate to http://localhost:8082/docmgmt

#### Problem: Port 8082 already in use

**Solution:** 
1. Find process using port: `lsof -i :8082`
2. Kill process: `kill -9 <PID>`
3. Or change port in application settings

#### Problem: Database or data not found

**Solution:** Check data directory exists:
```bash
mkdir -p ~/Library/Application\ Support/DocMgmt/
```

## Customization

### Change Application Name

Edit `pom.xml`:
```xml
<properties>
    <app.name>YourAppName</app.name>
    <app.version>1.0.0</app.version>
    <app.vendor>Your Company</app.vendor>
</properties>
```

Also update `build-mac-dmg.sh`:
```bash
APP_NAME="YourAppName"
APP_VERSION="1.0.0"
```

### Change Server Port

Edit `build-mac-dmg.sh` or jpackage command:
```bash
--java-options "-Dserver.port=8080"
```

### Increase Memory Allocation

Edit `build-mac-dmg.sh`:
```bash
--java-options "-Xmx4096m"  # 4GB instead of 2GB
```

### Add Custom JVM Options

Add more `--java-options` arguments:
```bash
--java-options "-Dmy.custom.property=value" \
--java-options "-Dspring.datasource.url=jdbc:h2:file:./custom_db"
```

## Advanced Topics

### Creating Multiple Build Variants

Create different profiles in `pom.xml`:

```xml
<profiles>
    <profile>
        <id>mac-app-enterprise</id>
        <properties>
            <app.name>DocMgmt-Enterprise</app.name>
            <app.version>1.0.0-ENT</app.version>
        </properties>
    </profile>
</profiles>
```

Build with: `mvn package -Pmac-app-enterprise`

### Automated Builds

Create CI/CD pipeline (e.g., GitHub Actions):

```yaml
name: Build macOS DMG
on: [push, pull_request]
jobs:
  build:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Build DMG
        run: ./build-mac-dmg.sh
      - uses: actions/upload-artifact@v2
        with:
          name: DocMgmt-DMG
          path: target/dist/*.dmg
```

### Bundling Additional Resources

Add files to the application bundle:

1. Create `packaging/macos/resources/` directory
2. Add resource files
3. Modify jpackage command:
   ```bash
   --resource-dir packaging/macos/resources
   ```

## Version Updates

### Updating Application Version

1. Update `pom.xml`:
   ```xml
   <version>1.1.0-SNAPSHOT</version>
   ```

2. Update `build-mac-dmg.sh`:
   ```bash
   APP_VERSION="1.1.0"
   ```

3. Rebuild:
   ```bash
   ./build-mac-dmg.sh
   ```

### Upgrading Dependencies

Update versions in `pom.xml`:
- Spring Boot: Update parent version
- Vaadin: Update `vaadin.version` property
- Java: Update `java.version` property

Then rebuild application.

## Support and Resources

### Documentation
- Main README: `README.md`
- API Documentation: http://localhost:8082/docmgmt/api/swagger-ui.html
- Icon Creation: `packaging/macos/CREATE_ICON.md`

### Tools
- [jpackage documentation](https://docs.oracle.com/en/java/javase/21/jpackage/)
- [Maven documentation](https://maven.apache.org/guides/)
- [Vaadin documentation](https://vaadin.com/docs/latest)

### Common Tasks

```bash
# Quick rebuild without clean
mvn package -Pproduction -DskipTests

# Build with tests
mvn package -Pproduction

# Run locally without DMG
mvn spring-boot:run

# Clean everything including caches
mvn clean
rm -rf target/ node_modules/ package-lock.json

# Check dependencies
mvn dependency:tree

# Update Maven dependencies
mvn versions:display-dependency-updates
```

## FAQ

**Q: How big is the DMG file?**
A: Approximately 250-350 MB, including the bundled Java runtime.

**Q: Can users run this without installing Java?**
A: Yes! The Java runtime is bundled in the application.

**Q: Where does the application store data?**
A: In `~/Library/Application Support/DocMgmt/` (follows macOS conventions)

**Q: Can I build on Apple Silicon (M1/M2)?**
A: Yes, the DMG will work on both Intel and Apple Silicon Macs.

**Q: How do I update the application?**
A: Users install new DMG over the old version. Data is preserved in Application Support folder.

**Q: Can I change the port after building?**
A: The port is fixed in the build. To change it, edit the build script and rebuild.

**Q: Do I need to sign the application?**
A: Not required for internal use, but recommended for public distribution.

**Q: How do I uninstall?**
A: Delete DocMgmt.app from Applications folder. Optionally delete `~/Library/Application Support/DocMgmt/`

## License

See LICENSE file for terms.
