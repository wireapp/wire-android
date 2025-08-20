#!/bin/bash

# Wire APK Wrapper Script
# This script decompiles a Wire APK, moves com/wire folder from smali to smali_classes2, 
# rebuilds the APK with "_wrapped" suffix, and optionally signs it

set -e

# Default values
SIGN_APK=false
KEYSTORE_PATH=""
KEY_ALIAS=""
KEYSTORE_PASSWORD=""
KEY_PASSWORD=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if required tools are available
check_dependencies() {
    print_status "Checking dependencies..."
    
    if ! command -v apktool &> /dev/null; then
        print_error "apktool is not installed or not in PATH"
        print_error "Please install apktool first. See README.md for installation instructions."
        exit 1
    fi
    
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed or not in PATH"
        print_error "Please install Java first. See README.md for installation instructions."
        exit 1
    fi
    
    # Check signing dependencies if signing is enabled
    if [ "$SIGN_APK" = true ]; then
        if ! command -v apksigner &> /dev/null; then
            print_error "apksigner is not installed or not in PATH"
            print_error "Please install Android SDK Build Tools."
            print_error "See README.md for installation instructions."
            exit 1
        fi
        
        if ! command -v keytool &> /dev/null; then
            print_error "keytool is not installed or not in PATH"
            print_error "Please install Java JDK (keytool is part of JDK)."
            exit 1
        fi
    fi
    
    print_status "All dependencies found"
}

# Function to parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -s|--sign)
                SIGN_APK=true
                shift
                ;;
            -k|--keystore)
                KEYSTORE_PATH="$2"
                shift 2
                ;;
            -a|--key-alias)
                KEY_ALIAS="$2"
                shift 2
                ;;
            -p|--keystore-password)
                KEYSTORE_PASSWORD="$2"
                shift 2
                ;;
            -P|--key-password)
                KEY_PASSWORD="$2"
                shift 2
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            -*)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                if [ -z "$APK_FILE" ]; then
                    APK_FILE="$1"
                else
                    print_error "Multiple APK files specified"
                    show_usage
                    exit 1
                fi
                shift
                ;;
        esac
    done
}

# Function to prompt for passwords securely
prompt_for_passwords() {
    if [ "$SIGN_APK" = true ]; then
        if [ -z "$KEYSTORE_PASSWORD" ]; then
            echo -n "Enter keystore password: "
            read -s KEYSTORE_PASSWORD
            echo
        fi
        
        if [ -z "$KEY_PASSWORD" ]; then
            echo -n "Enter key password (press Enter if same as keystore password): "
            read -s KEY_PASSWORD
            echo
            if [ -z "$KEY_PASSWORD" ]; then
                KEY_PASSWORD="$KEYSTORE_PASSWORD"
            fi
        fi
    fi
}

# Function to validate APK integrity
validate_apk_integrity() {
    local original_apk="$1"
    local wrapped_apk="$2"
    
    print_status "Validating APK integrity..."
    
    # Check if aapt is available for manifest comparison
    local has_aapt=false
    if command -v aapt &> /dev/null; then
        has_aapt=true
    elif command -v aapt2 &> /dev/null; then
        has_aapt=true
    fi
    
    # Create temporary directory for validation
    local temp_dir=$(mktemp -d)
    
    # Compare DEX file count
    print_status "Comparing DEX file count..."
    local original_dex_count=$(unzip -l "$original_apk" 2>/dev/null | grep "\.dex$" | wc -l | tr -d ' ')
    local wrapped_dex_count=$(unzip -l "$wrapped_apk" 2>/dev/null | grep "\.dex$" | wc -l | tr -d ' ')
    
    echo "Original APK DEX files: $original_dex_count"
    echo "Wrapped APK DEX files:  $wrapped_dex_count"
    
    if [ "$original_dex_count" -ne "$wrapped_dex_count" ]; then
        print_warning "DEX file count differs between original and wrapped APK"
        print_warning "This might indicate changes in class organization"
    else
        print_status "DEX file count matches ✓"
    fi
    
    # Compare DEX file sizes (indicates class content)
    print_status "Comparing DEX file sizes..."
    
    # Get DEX file sizes from both APKs
    local original_dex_size=$(unzip -l "$original_apk" 2>/dev/null | grep "\.dex$" | awk '{total += $1} END {print total+0}')
    local wrapped_dex_size=$(unzip -l "$wrapped_apk" 2>/dev/null | grep "\.dex$" | awk '{total += $1} END {print total+0}')
    
    echo "Original APK DEX size: $original_dex_size bytes"
    echo "Wrapped APK DEX size:  $wrapped_dex_size bytes"
    
    if [ "$original_dex_size" -eq "$wrapped_dex_size" ]; then
        print_status "DEX file sizes match exactly ✓"
    else
        local dex_size_diff=$((wrapped_dex_size - original_dex_size))
        local dex_size_diff_abs=$((dex_size_diff < 0 ? dex_size_diff * -1 : dex_size_diff))
        local dex_size_percent=$((dex_size_diff_abs * 100 / original_dex_size))
        
        if [ "$dex_size_percent" -lt 1 ]; then
            print_status "DEX file sizes are very similar (${dex_size_percent}% difference) ✓"
        elif [ "$dex_size_percent" -lt 5 ]; then
            print_warning "DEX file sizes differ by ${dex_size_percent}% ($dex_size_diff bytes)"
            print_warning "This is usually normal for recompiled APKs"
        else
            print_warning "DEX file sizes differ significantly by ${dex_size_percent}% ($dex_size_diff bytes)"
            print_warning "This might indicate missing or added classes"
        fi
    fi
    
    # Quick verification using dexdump if available
    if command -v dexdump &> /dev/null; then
        print_status "Comparing class counts using dexdump..."
        
        # Extract first DEX file from each APK for comparison
        unzip -j "$original_apk" "classes.dex" -d "$temp_dir" 2>/dev/null || true
        if [ -f "$temp_dir/classes.dex" ]; then
            local orig_class_count=$(dexdump -f "$temp_dir/classes.dex" 2>/dev/null | grep -c "^Class descriptor" || echo "N/A")
            mv "$temp_dir/classes.dex" "$temp_dir/original_classes.dex"
        else
            local orig_class_count="N/A"
        fi
        
        unzip -j "$wrapped_apk" "classes.dex" -d "$temp_dir" 2>/dev/null || true
        if [ -f "$temp_dir/classes.dex" ]; then
            local wrap_class_count=$(dexdump -f "$temp_dir/classes.dex" 2>/dev/null | grep -c "^Class descriptor" || echo "N/A")
        else
            local wrap_class_count="N/A"
        fi
        
        echo "Original APK classes (main DEX): $orig_class_count"
        echo "Wrapped APK classes (main DEX):  $wrap_class_count"
        
        if [ "$orig_class_count" != "N/A" ] && [ "$wrap_class_count" != "N/A" ]; then
            if [ "$orig_class_count" -eq "$wrap_class_count" ]; then
                print_status "Main DEX class count matches ✓"
            else
                print_warning "Main DEX class count differs (Original: $orig_class_count, Wrapped: $wrap_class_count)"
            fi
        fi
    else
        print_warning "dexdump not available - install Android SDK Build Tools for detailed class analysis"
    fi
    
    # Compare manifest if aapt is available
    if [ "$has_aapt" = true ]; then
        print_status "Comparing manifest files..."
        
        if command -v aapt &> /dev/null; then
            aapt dump badging "$original_apk" > "$temp_dir/original_manifest.txt" 2>/dev/null
            aapt dump badging "$wrapped_apk" > "$temp_dir/wrapped_manifest.txt" 2>/dev/null
        else
            aapt2 dump badging "$original_apk" > "$temp_dir/original_manifest.txt" 2>/dev/null
            aapt2 dump badging "$wrapped_apk" > "$temp_dir/wrapped_manifest.txt" 2>/dev/null
        fi
        
        if [ -f "$temp_dir/original_manifest.txt" ] && [ -f "$temp_dir/wrapped_manifest.txt" ]; then
            local manifest_diff=$(diff "$temp_dir/original_manifest.txt" "$temp_dir/wrapped_manifest.txt" | wc -l | tr -d ' ')
            
            if [ "$manifest_diff" -eq 0 ]; then
                print_status "Manifest files are identical ✓"
            else
                print_warning "Manifest files differ ($manifest_diff lines changed)"
                print_warning "This is usually normal for recompiled APKs"
                
                # Show key differences
                echo "Key manifest differences:"
                diff "$temp_dir/original_manifest.txt" "$temp_dir/wrapped_manifest.txt" | head -10
            fi
        else
            print_warning "Could not extract manifest information for comparison"
        fi
    else
        print_warning "aapt/aapt2 not found - skipping manifest comparison"
        print_warning "Install Android SDK Build Tools to enable manifest validation"
    fi
    
    # Compare file entry counts
    print_status "Comparing file entry counts..."
    local original_entries=$(unzip -l "$original_apk" 2>/dev/null | tail -1 | awk '{print $2}')
    local wrapped_entries=$(unzip -l "$wrapped_apk" 2>/dev/null | tail -1 | awk '{print $2}')
    
    echo "Original APK entries: $original_entries"
    echo "Wrapped APK entries:  $wrapped_entries"
    
    if [ "$original_entries" -ne "$wrapped_entries" ]; then
        local entry_diff=$((wrapped_entries - original_entries))
        if [ "$entry_diff" -gt 0 ]; then
            print_warning "Wrapped APK has $entry_diff more entries than original"
        else
            print_warning "Wrapped APK has $((entry_diff * -1)) fewer entries than original"
        fi
    else
        print_status "File entry count matches ✓"
    fi
    
    # Check for critical components
    print_status "Checking for critical APK components..."
    
    local has_manifest_original=$(unzip -l "$original_apk" 2>/dev/null | grep -c "AndroidManifest.xml")
    local has_manifest_wrapped=$(unzip -l "$wrapped_apk" 2>/dev/null | grep -c "AndroidManifest.xml")
    
    if [ "$has_manifest_original" -eq "$has_manifest_wrapped" ] && [ "$has_manifest_wrapped" -gt 0 ]; then
        print_status "AndroidManifest.xml present ✓"
    else
        print_error "AndroidManifest.xml missing or count mismatch"
    fi
    
    local has_resources_original=$(unzip -l "$original_apk" 2>/dev/null | grep -c "resources.arsc")
    local has_resources_wrapped=$(unzip -l "$wrapped_apk" 2>/dev/null | grep -c "resources.arsc")
    
    if [ "$has_resources_original" -eq "$has_resources_wrapped" ]; then
        print_status "Resources structure preserved ✓"
    else
        print_warning "Resources structure differs"
    fi
    
    # Cleanup
    rm -rf "$temp_dir"
    
    print_status "APK integrity validation completed"
}

# Function to sign APK
sign_apk() {
    local unsigned_apk="$1"
    local signed_apk="$2"
    
    print_status "Step 5: Signing APK..."
    
    # Validate keystore file
    if [ ! -f "$KEYSTORE_PATH" ]; then
        print_error "Keystore file not found: $KEYSTORE_PATH"
        exit 1
    fi
    
    # Check if key alias exists in keystore
    if ! keytool -list -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS" -storepass "$KEYSTORE_PASSWORD" &>/dev/null; then
        print_error "Key alias '$KEY_ALIAS' not found in keystore"
        exit 1
    fi
    
    # Use apksigner
    print_status "Signing APK with apksigner..."
    if [ "$KEYSTORE_PASSWORD" = "$KEY_PASSWORD" ]; then
        apksigner sign --ks "$KEYSTORE_PATH" --ks-key-alias "$KEY_ALIAS" --ks-pass pass:"$KEYSTORE_PASSWORD" --out "$signed_apk" "$unsigned_apk"
    else
        apksigner sign --ks "$KEYSTORE_PATH" --ks-key-alias "$KEY_ALIAS" --ks-pass pass:"$KEYSTORE_PASSWORD" --key-pass pass:"$KEY_PASSWORD" --out "$signed_apk" "$unsigned_apk"
    fi
    
    if [ $? -eq 0 ]; then
        print_status "APK signed successfully: $signed_apk"
        
        # Verify the signature
        apksigner verify "$signed_apk" && print_status "APK signature verified"
    else
        print_error "Failed to sign APK"
        exit 1
    fi
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS] <path_to_wire_apk>"
    echo ""
    echo "Options:"
    echo "  -s, --sign                 Sign the APK after wrapping"
    echo "  -k, --keystore PATH        Path to keystore file (required for signing)"
    echo "  -a, --key-alias ALIAS      Key alias in keystore (required for signing)"
    echo "  -p, --keystore-password    Keystore password (will prompt if not provided)"
    echo "  -P, --key-password         Key password (will prompt if not provided)"
    echo "  -h, --help                Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 wire.apk"
    echo "  $0 --sign --keystore my-key.jks --key-alias mykey wire.apk"
    echo "  $0 -s -k my-key.jks -a mykey wire.apk"
    echo ""
    echo "This script will:"
    echo "1. Decompile the APK using apktool"
    echo "2. Move com/wire folder from smali to smali_classes2"
    echo "3. Rebuild the APK"
    echo "4. Name it with '_wrapped' suffix"
    echo "5. Validate APK integrity (compare classes, manifest, etc.)"
    echo "6. Optionally sign the APK if --sign is specified"
}

# Main function
main() {
    # Parse command line arguments
    parse_arguments "$@"
    
    # Check if APK file is provided
    if [ -z "$APK_FILE" ]; then
        print_error "No APK file provided"
        show_usage
        exit 1
    fi
    
    # Check if APK file exists
    if [ ! -f "$APK_FILE" ]; then
        print_error "APK file not found: $APK_FILE"
        exit 1
    fi
    
    # Validate signing parameters
    if [ "$SIGN_APK" = true ]; then
        if [ -z "$KEYSTORE_PATH" ]; then
            print_error "Keystore path is required for signing (use -k or --keystore)"
            exit 1
        fi
        
        if [ -z "$KEY_ALIAS" ]; then
            print_error "Key alias is required for signing (use -a or --key-alias)"
            exit 1
        fi
    fi
    
    # Check dependencies
    check_dependencies
    
    # Prompt for passwords if signing is enabled
    prompt_for_passwords
    
    # Get APK filename without extension
    APK_BASENAME=$(basename "$APK_FILE" .apk)
    APK_DIR=$(dirname "$APK_FILE")
    WORK_DIR="${APK_DIR}/${APK_BASENAME}_work"
    OUTPUT_APK="${APK_DIR}/${APK_BASENAME}_wrapped.apk"
    SIGNED_APK="${APK_DIR}/${APK_BASENAME}_wrapped_signed.apk"
    
    print_status "Processing APK: $APK_FILE"
    print_status "Work directory: $WORK_DIR"
    print_status "Output APK: $OUTPUT_APK"
    if [ "$SIGN_APK" = true ]; then
        print_status "Signed APK: $SIGNED_APK"
    fi
    
    # Clean up any existing work directory
    if [ -d "$WORK_DIR" ]; then
        print_warning "Removing existing work directory: $WORK_DIR"
        rm -rf "$WORK_DIR"
    fi
    
    # Step 1: Decompile APK
    print_status "Step 1: Decompiling APK with apktool..."
    apktool d "$APK_FILE" -o "$WORK_DIR"
    
    if [ $? -ne 0 ]; then
        print_error "Failed to decompile APK"
        exit 1
    fi
    
    # Step 2: Check if smali and smali_classes2 directories exist
    SMALI_DIR="$WORK_DIR/smali"
    SMALI_CLASSES2_DIR="$WORK_DIR/smali_classes2"
    
    if [ ! -d "$SMALI_DIR" ]; then
        print_error "smali directory not found: $SMALI_DIR"
        exit 1
    fi
    
    # Create smali_classes2 directory if it doesn't exist
    if [ ! -d "$SMALI_CLASSES2_DIR" ]; then
        print_status "Creating smali_classes2 directory..."
        mkdir -p "$SMALI_CLASSES2_DIR"
    fi
    
    # Step 3: Move com/wire folder
    WIRE_SMALI_DIR="$SMALI_DIR/com/wire"
    WIRE_SMALI_CLASSES2_DIR="$SMALI_CLASSES2_DIR/com/wire"
    
    if [ ! -d "$WIRE_SMALI_DIR" ]; then
        print_error "com/wire directory not found in smali: $WIRE_SMALI_DIR"
        exit 1
    fi
    
    print_status "Step 2: Moving com/wire from smali to smali_classes2..."
    
    # Create com directory in smali_classes2 if it doesn't exist
    mkdir -p "$SMALI_CLASSES2_DIR/com"
    
    # Move the wire directory
    mv "$WIRE_SMALI_DIR" "$WIRE_SMALI_CLASSES2_DIR"
    
    if [ $? -ne 0 ]; then
        print_error "Failed to move com/wire directory"
        exit 1
    fi
    
    print_status "Successfully moved com/wire to smali_classes2"
    
    # Step 4: Rebuild APK
    print_status "Step 3: Rebuilding APK..."
    apktool b "$WORK_DIR" -o "$OUTPUT_APK"
    
    if [ $? -ne 0 ]; then
        print_error "Failed to rebuild APK"
        exit 1
    fi
    
    # Step 5: Validate APK integrity
    validate_apk_integrity "$APK_FILE" "$OUTPUT_APK"
    
    # Step 6: Sign APK if requested
    if [ "$SIGN_APK" = true ]; then
        sign_apk "$OUTPUT_APK" "$SIGNED_APK"
    fi
    
    # Step 7: Clean up work directory
    STEP_NUM=$((SIGN_APK ? 7 : 6))
    print_status "Step $STEP_NUM: Cleaning up work directory..."
    rm -rf "$WORK_DIR"
    
    print_status "Process completed successfully!"
    print_status "Output APK: $OUTPUT_APK"
    if [ "$SIGN_APK" = true ]; then
        print_status "Signed APK: $SIGNED_APK"
    fi
    
    # Show file size comparison
    ORIGINAL_SIZE=$(stat -f%z "$APK_FILE" 2>/dev/null || stat -c%s "$APK_FILE" 2>/dev/null || echo "unknown")
    OUTPUT_SIZE=$(stat -f%z "$OUTPUT_APK" 2>/dev/null || stat -c%s "$OUTPUT_APK" 2>/dev/null || echo "unknown")
    
    echo ""
    echo "File size comparison:"
    echo "Original APK: $ORIGINAL_SIZE bytes"
    echo "Wrapped APK:  $OUTPUT_SIZE bytes"
    
    if [ "$SIGN_APK" = true ]; then
        SIGNED_SIZE=$(stat -f%z "$SIGNED_APK" 2>/dev/null || stat -c%s "$SIGNED_APK" 2>/dev/null || echo "unknown")
        echo "Signed APK:   $SIGNED_SIZE bytes"
        print_status "The signed APK is ready for installation"
    else
        print_warning "Note: The output APK is unsigned and may need to be signed before installation"
    fi
}

# Run main function with all arguments
main "$@"