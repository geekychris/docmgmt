# Quick Start: macOS DMG Build

## TL;DR

```bash
# Build DMG installer
./build-mac-dmg.sh

# Output: target/dist/DocMgmt-1.0.0.dmg
```

## Prerequisites

```bash
# Check prerequisites
java -version        # Need: Java 21+
mvn -version         # Need: Maven 3.6+
jpackage --version   # Need: jpackage (included in JDK 14+)
```

Install if missing:
```bash
brew install openjdk@21 maven
```

## Build Steps

### 1. Optional: Create App Icon
```bash
cd packaging/macos
./create-default-icon.sh
cd ../..
```

### 2. Build DMG
```bash
./build-mac-dmg.sh
```

### 3. Test
```bash
# Open and install
open target/dist/DocMgmt-1.0.0.dmg

# Drag to Applications, then run
open /Applications/DocMgmt.app
```

## What You Get

- **DMG File**: `target/dist/DocMgmt-1.0.0.dmg` (~250-350 MB)
- **Bundled Java**: No separate Java installation needed
- **Self-contained**: All dependencies included
- **macOS Native**: Proper .app bundle with icon

## Installation for End Users

1. Download `DocMgmt-1.0.0.dmg`
2. Double-click to open
3. Drag DocMgmt to Applications folder
4. Double-click DocMgmt in Applications
5. Browser opens automatically to http://localhost:8082/docmgmt

## Common Issues

### Security Warning
```bash
# If macOS blocks the app
xattr -dr com.apple.quarantine /Applications/DocMgmt.app
```

### Port Already in Use
```bash
# Kill process on port 8082
lsof -i :8082
kill -9 <PID>
```

### Build Fails
```bash
# Clean and rebuild
mvn clean
rm -rf target/
./build-mac-dmg.sh
```

## Customization

Edit `build-mac-dmg.sh` to change:
- `APP_NAME`: Application name
- `APP_VERSION`: Version number
- Port, memory, JVM options

## Full Documentation

See `BUILD_MAC.md` for complete documentation including:
- Detailed build process
- Troubleshooting guide
- Code signing and notarization
- Distribution strategies
- Advanced customization

## Quick Commands

```bash
# Build DMG
./build-mac-dmg.sh

# Run locally (development)
mvn spring-boot:run

# Build JAR only
mvn package -Pproduction -DskipTests

# Clean everything
mvn clean && rm -rf target/

# Create custom icon
cd packaging/macos && ./create-default-icon.sh
```

## Data Storage

Application data stored in:
```
~/Library/Application Support/DocMgmt/
├── docmgmt_db.mv.db    # Database
├── lucene_index/        # Search index
└── logs/                # Application logs
```

## Permissions

The app is configured with proper macOS permissions for:
- ✅ Filesystem access (Application Support folder)
- ✅ Network access (web server on port 8082)
- ✅ Java runtime (JIT, native libraries)

See `MACOS_PERMISSIONS.md` for complete details on security and permissions.

## Support

- Full guide: `BUILD_MAC.md`
- Permissions: `MACOS_PERMISSIONS.md`
- Main README: `README.md`
- Icon help: `packaging/macos/CREATE_ICON.md`
