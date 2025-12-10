#!/bin/bash

# Simple macOS Application Builder
# Creates a .app bundle without jpackage for better server application support

set -e

echo "========================================="
echo "DocMgmt Simple macOS App Builder"
echo "========================================="
echo ""

APP_NAME="DocMgmt"
APP_VERSION="1.0.0"
BUILD_DIR="target/mac-app"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"

# Clean previous build
echo "Cleaning previous build..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# Always do a clean build to ensure fresh Vaadin frontend
echo "Building application with clean frontend build..."
mvn clean package -Pproduction -DskipTests

# Create app bundle structure
echo "Creating application bundle..."
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"
mkdir -p "$APP_BUNDLE/Contents/Java"

# Copy JAR
echo "Copying JAR file..."
cp target/document-management-0.0.1-SNAPSHOT.jar "$APP_BUNDLE/Contents/Java/"

# Create launcher script
echo "Creating launcher script..."
cat > "$APP_BUNDLE/Contents/MacOS/$APP_NAME" << 'EOF'
#!/bin/bash

# Get the directory of the app bundle
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
JAR_FILE="$DIR/Java/document-management-0.0.1-SNAPSHOT.jar"

# Application data directory
APP_DATA_DIR="$HOME/Library/Application Support/DocMgmt"
mkdir -p "$APP_DATA_DIR/logs"

# Log file for debugging
LOG_FILE="$APP_DATA_DIR/logs/startup.log"

# Start logging
{
    echo "=================================="
    echo "DocMgmt Startup - $(date)"
    echo "=================================="
    echo "Bundle: $DIR"
    echo "JAR: $JAR_FILE"
    echo "Data Dir: $APP_DATA_DIR"
    echo ""

    # Check if JAR exists
    if [ ! -f "$JAR_FILE" ]; then
        echo "ERROR: JAR file not found at $JAR_FILE"
        osascript -e 'display dialog "Error: Application files not found" buttons {"OK"} with icon stop'
        exit 1
    fi

    # Change to data directory
    cd "$APP_DATA_DIR"

    # Java options (using array to handle paths with spaces)
    JAVA_OPTS=(
        "-Xmx2048m"
        "-Xms512m"
        "-Dserver.port=8082"
        "-Dspring.profiles.active=production"
        "-Dspring.datasource.url=jdbc:h2:file:${APP_DATA_DIR}/docmgmt_db"
        "-Dlucene.index.path=${APP_DATA_DIR}/lucene_index"
        "-Dlogging.file.name=${APP_DATA_DIR}/logs/application.log"
    )

    echo "Starting server..."
    echo "Command: java ${JAVA_OPTS[*]} -jar $JAR_FILE"
    echo ""
    
    # Start server
    java "${JAVA_OPTS[@]}" -jar "$JAR_FILE" &
    SERVER_PID=$!
    
    echo "Server PID: $SERVER_PID"
    
    # Wait for server to start
    echo "Waiting for server to start..."
    sleep 8
    
    # Check if server is running
    if ps -p $SERVER_PID > /dev/null 2>&1; then
        echo "Server started successfully!"
        
        # Open browser
        open "http://localhost:8082/docmgmt"
        
        # Show notification
        osascript -e 'display notification "Server is running on port 8082" with title "DocMgmt" subtitle "Browser opening..."'
        
        echo ""
        echo "Application is running!"
        echo "Web UI: http://localhost:8082/docmgmt"
        echo "To stop: kill $SERVER_PID"
    else
        echo "ERROR: Server failed to start"
        echo "Check application.log for details"
        osascript -e 'display dialog "Server failed to start.\n\nCheck logs at:\n'"$APP_DATA_DIR/logs"'" buttons {"OK"} with icon caution'
    fi
    
} > "$LOG_FILE" 2>&1

# Keep script running to keep app active in Dock
wait $SERVER_PID
EOF

chmod +x "$APP_BUNDLE/Contents/MacOS/$APP_NAME"

# Create Info.plist
echo "Creating Info.plist..."
cat > "$APP_BUNDLE/Contents/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>$APP_NAME</string>
    <key>CFBundleDisplayName</key>
    <string>Document Management</string>
    <key>CFBundleIdentifier</key>
    <string>com.docmgmt.app</string>
    <key>CFBundleVersion</key>
    <string>$APP_VERSION</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleExecutable</key>
    <string>$APP_NAME</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.14</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>LSUIElement</key>
    <false/>
    <key>NSAppleEventsUsageDescription</key>
    <string>DocMgmt needs to open your web browser.</string>
</dict>
</plist>
EOF

# Copy icon if exists
if [ -f "packaging/macos/app-icon.icns" ]; then
    echo "Copying application icon..."
    cp packaging/macos/app-icon.icns "$APP_BUNDLE/Contents/Resources/$APP_NAME.icns"
    # Update Info.plist with icon
    /usr/libexec/PlistBuddy -c "Add :CFBundleIconFile string '$APP_NAME.icns'" "$APP_BUNDLE/Contents/Info.plist" 2>/dev/null || true
fi

# Create DMG
echo ""
echo "Creating DMG..."
DMG_FILE="$BUILD_DIR/$APP_NAME-$APP_VERSION.dmg"
rm -f "$DMG_FILE"

# Create temporary DMG folder with nice layout
DMG_TEMP="$BUILD_DIR/dmg-temp"
rm -rf "$DMG_TEMP"
mkdir -p "$DMG_TEMP"

# Copy app to temp folder
cp -R "$APP_BUNDLE" "$DMG_TEMP/"

# Create Applications symlink
ln -s /Applications "$DMG_TEMP/Applications"

# Create DMG
hdiutil create -volname "$APP_NAME" \
               -srcfolder "$DMG_TEMP" \
               -ov \
               -format UDZO \
               "$DMG_FILE"

# Clean up temp folder
rm -rf "$DMG_TEMP"

# Get file size
DMG_SIZE=$(du -h "$DMG_FILE" | cut -f1)

echo ""
echo "========================================="
echo "Build Complete!"
echo "========================================="
echo "DMG Location: $DMG_FILE"
echo "DMG Size: $DMG_SIZE"
echo ""
echo "To install:"
echo "  1. Open $DMG_FILE"
echo "  2. Drag $APP_NAME.app to Applications folder"
echo "  3. Double-click to run"
echo ""
echo "To test now:"
echo "  open $APP_BUNDLE"
echo ""
