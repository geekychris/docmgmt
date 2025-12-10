#!/bin/bash

# Build macOS DMG installer for Document Management System
# This script builds the application and creates a DMG installer

set -e  # Exit on error

echo "========================================="
echo "DocMgmt macOS DMG Builder"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="DocMgmt"
APP_VERSION="1.0.0"
DMG_NAME="DocMgmt-${APP_VERSION}"

# Check prerequisites
echo "Checking prerequisites..."

# Check Java version
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java not found. Please install Java 21 or later.${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}❌ Java 21 or later required. Current version: $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven not found. Please install Maven 3.6 or later.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Maven$(mvn -version | head -n1 | cut -d' ' -f3)${NC}"

# Check for jpackage (comes with JDK 14+)
if ! command -v jpackage &> /dev/null; then
    echo -e "${RED}❌ jpackage not found. Please ensure you're using JDK 14 or later.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ jpackage available${NC}"

# Check for app icon
if [ ! -f "packaging/macos/app-icon.icns" ]; then
    echo -e "${YELLOW}⚠ App icon not found. Using default Java icon.${NC}"
    echo -e "${YELLOW}  To create a custom icon, see: packaging/macos/CREATE_ICON.md${NC}"
    echo ""
    read -p "Continue without custom icon? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted. Please create an icon first."
        exit 1
    fi
    
    # Create a minimal configuration without icon
    SKIP_ICON=true
else
    echo -e "${GREEN}✓ App icon found${NC}"
    SKIP_ICON=false
fi

echo ""
echo "========================================="
echo "Step 1: Clean previous builds"
echo "========================================="
mvn clean

echo ""
echo "========================================="
echo "Step 2: Build application (production mode)"
echo "========================================="
mvn package -Pproduction -DskipTests

# Check if JAR was created
if [ ! -f "target/document-management-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${RED}❌ Build failed - JAR file not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Application JAR built successfully${NC}"

echo ""
echo "========================================="
echo "Step 3: Create macOS application bundle"
echo "========================================="

# Prepare jpackage arguments
JPACKAGE_ARGS=(
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "DocMgmt Project"
    --description "Document Management System with versioning and full-text search"
    --type dmg
    --input target
    --main-jar document-management-0.0.1-SNAPSHOT.jar
    --main-class com.docmgmt.DocumentManagementApplication
    --dest target/dist
    --java-options "-Xmx2048m"
    --java-options "-Xms512m"
    --java-options "-Dserver.port=8082"
    --java-options "-Dspring.profiles.active=production"
    --java-options "-Djava.io.tmpdir=\$TMPDIR"
    --java-options "-Duser.home=\$HOME"
    --mac-package-name "$APP_NAME"
    --mac-package-identifier "com.docmgmt.app"
    --mac-entitlements packaging/macos/entitlements.plist
    --resource-dir packaging/macos
)

# Add icon if available
if [ "$SKIP_ICON" = false ]; then
    JPACKAGE_ARGS+=(--icon packaging/macos/app-icon.icns)
fi

# Run jpackage
jpackage "${JPACKAGE_ARGS[@]}"

# Check if DMG was created
DMG_FILE="target/dist/${APP_NAME}-${APP_VERSION}.dmg"
if [ ! -f "$DMG_FILE" ]; then
    echo -e "${RED}❌ DMG creation failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ DMG created successfully${NC}"

# Get file size
DMG_SIZE=$(du -h "$DMG_FILE" | cut -f1)

echo ""
echo "========================================="
echo "Build Complete!"
echo "========================================="
echo -e "${GREEN}DMG Location:${NC} $DMG_FILE"
echo -e "${GREEN}DMG Size:${NC} $DMG_SIZE"
echo ""
echo "To install:"
echo "  1. Open the DMG file"
echo "  2. Drag ${APP_NAME}.app to your Applications folder"
echo "  3. Double-click the app to run"
echo ""
echo "The application will:"
echo "  - Store data in ~/Library/Application Support/${APP_NAME}/"
echo "  - Start a web server on http://localhost:8082/docmgmt"
echo "  - Open your default browser automatically"
echo ""
echo "To distribute:"
echo "  - Share the DMG file: $DMG_FILE"
echo "  - Users can install by dragging to Applications"
echo "  - No additional dependencies required (Java is bundled)"
echo ""
