#!/bin/bash

# Wire Android - Release Notes Preparation Script
# This script prepares release notes for Play Store deployment by:
# 1. Extracting the version from AndroidCoordinates.kt
# 2. Validating that version-specific release notes exist (fails if missing)
# 3. Copying version-specific files to default.txt for deployment
# 4. Validating character count (500 character limit)
# 5. Displaying preview of release notes to be deployed

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Wire Android Release Notes Preparation ===${NC}"
echo ""

# Path to AndroidCoordinates.kt
COORDINATES_FILE="$PROJECT_ROOT/build-logic/plugins/src/main/kotlin/AndroidCoordinates.kt"

# Extract version name from AndroidCoordinates.kt
if [ ! -f "$COORDINATES_FILE" ]; then
    echo -e "${RED}Error: AndroidCoordinates.kt not found at $COORDINATES_FILE${NC}"
    exit 1
fi

VERSION=$(awk '/^[[:space:]]*const[[:space:]]+val[[:space:]]+versionName[[:space:]]*=[[:space:]]*"/ { match($0, /"([^"]+)"/, arr); print arr[1] }' "$COORDINATES_FILE")

if [ -z "$VERSION" ]; then
    echo -e "${RED}Error: Could not extract version from AndroidCoordinates.kt${NC}"
    exit 1
fi

echo -e "${GREEN}Detected version: ${VERSION}${NC}"
echo ""

# Release notes directory
RELEASE_NOTES_DIR="$PROJECT_ROOT/app/src/main/play/release-notes"

# Play Store character limit for release notes
MAX_CHARACTERS=500

# Languages to process
LANGUAGES=("en-US" "de-DE")

# Process each language
for LANG in "${LANGUAGES[@]}"; do
    LANG_DIR="$RELEASE_NOTES_DIR/$LANG"
    VERSION_FILE="$LANG_DIR/${VERSION}.txt"
    DEFAULT_FILE="$LANG_DIR/default.txt"

    echo -e "${BLUE}Processing $LANG...${NC}"

    if [ ! -d "$LANG_DIR" ]; then
        echo -e "${YELLOW}Warning: Language directory not found: $LANG_DIR${NC}"
        echo -e "${YELLOW}Skipping $LANG${NC}"
        echo ""
        continue
    fi

    if [ -f "$VERSION_FILE" ]; then
        echo -e "${GREEN}✓ Found version-specific release notes: ${VERSION}.txt${NC}"

        # Copy version-specific file to default.txt
        cp "$VERSION_FILE" "$DEFAULT_FILE"
        echo -e "${GREEN}  Copied ${VERSION}.txt to default.txt${NC}"

    else
        echo -e "${RED}✗ Error: Version-specific release notes not found: ${VERSION}.txt${NC}"
        echo -e "${RED}  Release notes for version ${VERSION} must be created before deployment${NC}"
        echo -e "${RED}  Expected file: $VERSION_FILE${NC}"
        exit 1
    fi

    echo ""
done

# Validate character counts
echo -e "${BLUE}=== Validating Character Counts ===${NC}"
VALIDATION_FAILED=false

for LANG in "${LANGUAGES[@]}"; do
    DEFAULT_FILE="$RELEASE_NOTES_DIR/$LANG/default.txt"

    if [ -f "$DEFAULT_FILE" ]; then
        CHAR_COUNT=$(python3 -c "import sys; print(len(sys.stdin.read()))" < "$DEFAULT_FILE")

        echo -e "${BLUE}[$LANG]${NC} Character count: ${CHAR_COUNT}/${MAX_CHARACTERS}"

        if [ "$CHAR_COUNT" -gt "$MAX_CHARACTERS" ]; then
            OVERFLOW=$((CHAR_COUNT - MAX_CHARACTERS))
            echo -e "${RED}✗ FAILED: Exceeds limit by ${OVERFLOW} characters${NC}"
            VALIDATION_FAILED=true
        elif [ "$CHAR_COUNT" -eq "$MAX_CHARACTERS" ]; then
            echo -e "${YELLOW}⚠ WARNING: Exactly at character limit${NC}"
        else
            REMAINING=$((MAX_CHARACTERS - CHAR_COUNT))
            echo -e "${GREEN}✓ PASSED: ${REMAINING} characters remaining${NC}"
        fi
        echo ""
    fi
done

if [ "$VALIDATION_FAILED" = true ]; then
    echo -e "${RED}Error: One or more release notes exceed the Play Store ${MAX_CHARACTERS} character limit${NC}"
    echo -e "${RED}Please reduce the content and try again${NC}"
    exit 1
fi

# Summary
echo -e "${BLUE}=== Summary ===${NC}"
echo -e "${GREEN}✓ Using version-specific release notes for version ${VERSION}${NC}"
echo ""
echo -e "${GREEN}Release notes preparation completed successfully!${NC}"

# Display the release notes that will be used
echo ""
echo -e "${BLUE}=== Release Notes Preview ===${NC}"
for LANG in "${LANGUAGES[@]}"; do
    DEFAULT_FILE="$RELEASE_NOTES_DIR/$LANG/default.txt"
    if [ -f "$DEFAULT_FILE" ]; then
        echo ""
        echo -e "${BLUE}[$LANG]${NC}"
        echo "---"
        cat "$DEFAULT_FILE"
        echo "---"
    fi
done
