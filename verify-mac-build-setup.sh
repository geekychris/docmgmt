#!/bin/bash

# Verify macOS Build Setup
# This script checks that all prerequisites are met for building the macOS DMG

echo "================================================"
echo "macOS DMG Build Setup Verification"
echo "================================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ERRORS=0
WARNINGS=0

# Check macOS
echo "Checking Operating System..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    MACOS_VERSION=$(sw_vers -productVersion)
    echo -e "${GREEN}✓ macOS $MACOS_VERSION${NC}"
else
    echo -e "${RED}✗ Not running on macOS${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Check Java
echo "Checking Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
    
    if [ "$JAVA_MAJOR" -ge 21 ]; then
        echo -e "${GREEN}✓ Java $JAVA_VERSION (>= 21 required)${NC}"
    else
        echo -e "${RED}✗ Java $JAVA_VERSION (21+ required)${NC}"
        echo "  Install with: brew install openjdk@21"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${RED}✗ Java not found${NC}"
    echo "  Install with: brew install openjdk@21"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Check Maven
echo "Checking Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version 2>&1 | head -n1 | awk '{print $3}')
    echo -e "${GREEN}✓ Maven $MVN_VERSION${NC}"
else
    echo -e "${RED}✗ Maven not found${NC}"
    echo "  Install with: brew install maven"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Check jpackage
echo "Checking jpackage..."
if command -v jpackage &> /dev/null; then
    echo -e "${GREEN}✓ jpackage available${NC}"
else
    echo -e "${RED}✗ jpackage not found${NC}"
    echo "  jpackage comes with JDK 14+"
    echo "  Make sure you're using a recent JDK"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Check Homebrew (optional but helpful)
echo "Checking Homebrew (optional)..."
if command -v brew &> /dev/null; then
    BREW_VERSION=$(brew --version | head -n1 | awk '{print $2}')
    echo -e "${GREEN}✓ Homebrew $BREW_VERSION${NC}"
else
    echo -e "${YELLOW}⚠ Homebrew not found (optional but recommended)${NC}"
    echo "  Install from: https://brew.sh"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Check ImageMagick (optional for icon creation)
echo "Checking ImageMagick (optional for icon creation)..."
if command -v convert &> /dev/null; then
    IM_VERSION=$(convert -version | head -n1 | awk '{print $3}')
    echo -e "${GREEN}✓ ImageMagick $IM_VERSION${NC}"
else
    echo -e "${YELLOW}⚠ ImageMagick not found (optional)${NC}"
    echo "  Needed for: packaging/macos/create-default-icon.sh"
    echo "  Install with: brew install imagemagick"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Check disk space
echo "Checking disk space..."
AVAILABLE_GB=$(df -h . | tail -n1 | awk '{print $4}' | sed 's/Gi//')
AVAILABLE_MB=$(df -m . | tail -n1 | awk '{print $4}')

if [ "$AVAILABLE_MB" -gt 4096 ]; then
    echo -e "${GREEN}✓ ${AVAILABLE_GB} available (4GB+ required)${NC}"
else
    echo -e "${YELLOW}⚠ Only ${AVAILABLE_GB} available (4GB+ recommended)${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Check project files
echo "Checking project files..."
FILES_MISSING=0

if [ -f "pom.xml" ]; then
    echo -e "${GREEN}✓ pom.xml${NC}"
else
    echo -e "${RED}✗ pom.xml not found${NC}"
    FILES_MISSING=$((FILES_MISSING + 1))
fi

if [ -f "build-mac-dmg.sh" ]; then
    echo -e "${GREEN}✓ build-mac-dmg.sh${NC}"
else
    echo -e "${RED}✗ build-mac-dmg.sh not found${NC}"
    FILES_MISSING=$((FILES_MISSING + 1))
fi

if [ -d "packaging/macos" ]; then
    echo -e "${GREEN}✓ packaging/macos/ directory${NC}"
else
    echo -e "${RED}✗ packaging/macos/ directory not found${NC}"
    FILES_MISSING=$((FILES_MISSING + 1))
fi

if [ -f "packaging/macos/CREATE_ICON.md" ]; then
    echo -e "${GREEN}✓ Icon creation guide${NC}"
else
    echo -e "${RED}✗ packaging/macos/CREATE_ICON.md not found${NC}"
    FILES_MISSING=$((FILES_MISSING + 1))
fi

if [ $FILES_MISSING -gt 0 ]; then
    ERRORS=$((ERRORS + 1))
fi
echo ""

# Check for custom icon
echo "Checking for custom icon..."
if [ -f "packaging/macos/app-icon.icns" ]; then
    ICON_SIZE=$(du -h packaging/macos/app-icon.icns | cut -f1)
    echo -e "${GREEN}✓ Custom icon found ($ICON_SIZE)${NC}"
else
    echo -e "${YELLOW}⚠ No custom icon (will use default Java icon)${NC}"
    echo "  Create one with: cd packaging/macos && ./create-default-icon.sh"
    echo "  Or see: packaging/macos/CREATE_ICON.md"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Summary
echo "================================================"
echo "Summary"
echo "================================================"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Ready to build.${NC}"
    echo ""
    echo "To build DMG, run:"
    echo "  ./build-mac-dmg.sh"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ $WARNINGS warning(s), but you can proceed.${NC}"
    echo ""
    echo "To build DMG, run:"
    echo "  ./build-mac-dmg.sh"
else
    echo -e "${RED}✗ $ERRORS error(s) must be fixed before building.${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ $WARNINGS warning(s) can be ignored.${NC}"
    fi
    echo ""
    echo "Fix the errors above and run this script again."
    exit 1
fi

echo ""
echo "Documentation:"
echo "  Quick Start: QUICK_START_MAC.md"
echo "  Full Guide:  BUILD_MAC.md"
echo ""
