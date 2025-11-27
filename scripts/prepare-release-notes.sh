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
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Detect OS and set echo command accordingly
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    ECHO_CMD="echo"
else
    # Linux and others
    ECHO_CMD="echo -e"
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

$ECHO_CMD "${BLUE}=== Wire Android Release Notes Preparation ===${NC}"
echo ""

# Path to AndroidCoordinates.kt
COORDINATES_FILE="$PROJECT_ROOT/build-logic/plugins/src/main/kotlin/AndroidCoordinates.kt"

# Extract version name from AndroidCoordinates.kt
if [ ! -f "$COORDINATES_FILE" ]; then
    $ECHO_CMD "${RED}Error: AndroidCoordinates.kt not found at $COORDINATES_FILE${NC}"
    exit 1
fi

VERSION=$(grep -E '^\s*const\s+val\s+versionName\s*=\s*"' "$COORDINATES_FILE" | sed -E 's/.*"([^"]+)".*/\1/')

if [ -z "$VERSION" ]; then
    $ECHO_CMD "${RED}Error: Could not extract version from AndroidCoordinates.kt${NC}"
    exit 1
fi

$ECHO_CMD "${GREEN}Detected version: ${VERSION}${NC}"
echo ""

# Release notes directory
RELEASE_NOTES_DIR="$PROJECT_ROOT/app/src/main/play/release-notes"

# Play Store character limit for release notes
MAX_CHARACTERS=500

# Languages to process
LANGUAGES=("en-US" "de-DE")

# Track if we're using version-specific release notes
USING_VERSION_SPECIFIC=true

# Process each language
for LANG in "${LANGUAGES[@]}"; do
    LANG_DIR="$RELEASE_NOTES_DIR/$LANG"
    VERSION_FILE="$LANG_DIR/${VERSION}.txt"
    DEFAULT_FILE="$LANG_DIR/default.txt"

    $ECHO_CMD "${BLUE}Processing $LANG...${NC}"

    if [ ! -d "$LANG_DIR" ]; then
        $ECHO_CMD "${YELLOW}Warning: Language directory not found: $LANG_DIR${NC}"
        $ECHO_CMD "${YELLOW}Skipping $LANG${NC}"
        echo ""
        continue
    fi

    if [ -f "$VERSION_FILE" ]; then
        $ECHO_CMD "${GREEN}✓ Found version-specific release notes: ${VERSION}.txt${NC}"

        # Copy version-specific file to default.txt
        cp "$VERSION_FILE" "$DEFAULT_FILE"
        $ECHO_CMD "${GREEN}  Copied ${VERSION}.txt to default.txt${NC}"

    else
        $ECHO_CMD "${YELLOW}⚠ Warning: Version-specific release notes not found: ${VERSION}.txt${NC}"
        USING_VERSION_SPECIFIC=false

        if [ -f "$DEFAULT_FILE" ]; then
            $ECHO_CMD "${YELLOW}  Using existing default.txt as fallback${NC}"
            $ECHO_CMD "${YELLOW}  Expected file: $VERSION_FILE${NC}"
        else
            $ECHO_CMD "${RED}✗ Error: Neither version-specific nor default release notes found${NC}"
            $ECHO_CMD "${RED}  Expected files:${NC}"
            $ECHO_CMD "${RED}    - $VERSION_FILE${NC}"
            $ECHO_CMD "${RED}    - $DEFAULT_FILE${NC}"
            exit 1
        fi
    fi

    echo ""
done

# Validate character counts
$ECHO_CMD "${BLUE}=== Validating Character Counts ===${NC}"
VALIDATION_FAILED=false

for LANG in "${LANGUAGES[@]}"; do
    DEFAULT_FILE="$RELEASE_NOTES_DIR/$LANG/default.txt"

    if [ -f "$DEFAULT_FILE" ]; then
        CHAR_COUNT=$(python3 -c "import sys; print(len(sys.stdin.read()))" < "$DEFAULT_FILE")

        $ECHO_CMD "${BLUE}[$LANG]${NC} Character count: ${CHAR_COUNT}/${MAX_CHARACTERS}"

        if [ "$CHAR_COUNT" -gt "$MAX_CHARACTERS" ]; then
            OVERFLOW=$((CHAR_COUNT - MAX_CHARACTERS))
            $ECHO_CMD "${RED}✗ FAILED: Exceeds limit by ${OVERFLOW} characters${NC}"
            VALIDATION_FAILED=true
        elif [ "$CHAR_COUNT" -eq "$MAX_CHARACTERS" ]; then
            $ECHO_CMD "${YELLOW}⚠ WARNING: Exactly at character limit${NC}"
        else
            REMAINING=$((MAX_CHARACTERS - CHAR_COUNT))
            $ECHO_CMD "${GREEN}✓ PASSED: ${REMAINING} characters remaining${NC}"
        fi
        echo ""
    fi
done

if [ "$VALIDATION_FAILED" = true ]; then
    $ECHO_CMD "${RED}Error: One or more release notes exceed the Play Store ${MAX_CHARACTERS} character limit${NC}"
    $ECHO_CMD "${RED}Please reduce the content and try again${NC}"
    exit 1
fi

# Summary
$ECHO_CMD "${BLUE}=== Summary ===${NC}"
if [ "$USING_VERSION_SPECIFIC" = true ]; then
    $ECHO_CMD "${GREEN}✓ Using version-specific release notes for version ${VERSION}${NC}"
else
    $ECHO_CMD "${YELLOW}⚠ Using fallback release notes (version-specific notes for ${VERSION} not found)${NC}"
fi
echo ""
$ECHO_CMD "${GREEN}Release notes preparation completed successfully!${NC}"

# Display the release notes that will be used
echo ""
$ECHO_CMD "${BLUE}=== Release Notes Preview ===${NC}"
for LANG in "${LANGUAGES[@]}"; do
    DEFAULT_FILE="$RELEASE_NOTES_DIR/$LANG/default.txt"
    if [ -f "$DEFAULT_FILE" ]; then
        echo ""
        $ECHO_CMD "${BLUE}[$LANG]${NC}"
        echo "---"
        cat "$DEFAULT_FILE"
        echo "---"
    fi
done
