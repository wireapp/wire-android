# Wire APK Wrapper Script

This script automates the process of modifying Wire APK files by moving the `com/wire` folder from `smali` to `smali_classes2` and rebuilding the APK.

## What It Does

1. **Decompiles** the Wire APK using apktool
2. **Moves** the `com/wire` folder from `smali` to `smali_classes2`
3. **Rebuilds** the APK with apktool
4. **Renames** the output APK with `_wrapped` suffix
5. **Optionally signs** the APK for installation (if `--sign` option is used)

## Prerequisites

### Required Tools

#### 1. Java Development Kit (JDK)
- **Required**: Java 11 or higher
- **Installation**:
  - **Windows**: `winget install Microsoft.OpenJDK.17` or download from [OpenJDK](https://openjdk.org/)
  - **macOS**: `brew install openjdk` or download from website
  - **Linux**: 
    - **Using SDKMAN** (recommended): `sdk install java 17.0.8-tem` ([SDKMAN](https://sdkman.io/))
    - **Ubuntu/Debian**: `sudo apt install default-jdk`
    - **CentOS/RHEL**: `sudo yum install java-17-openjdk`

#### 2. Apktool
- **Required**: Latest version (2.7.0 or higher recommended)
- **Download**: [Apktool Official GitHub](https://github.com/iBotPeaches/Apktool)
- **Purpose**: Core tool for decompiling and rebuilding APKs
- **Installation**:

  **macOS**:
  ```bash
  brew install apktool
  ```

  **Linux**:
  ```bash
  sudo apt update
  sudo apt install apktool
  ```

### Recommended Tools (for Enhanced Validation)

#### 3. Android SDK Build Tools
- **Purpose**: Provides `aapt` and `dexdump` for detailed APK analysis
- **Benefits**: 
  - Exact class counting with `dexdump`
  - Manifest comparison with `aapt`
  - Better validation of APK integrity
- **Download**: [Android SDK Command Line Tools](https://developer.android.com/studio#command-tools)
- **Installation**:

  **macOS**:
  ```bash
  # Using Homebrew (easiest method)
  brew install --cask android-commandlinetools
  
  # Or manually install
  # 1. Download commandline tools from Android Developer website
  # 2. Extract to ~/Library/Android/sdk/
  # 3. Add to PATH in ~/.zshrc or ~/.bash_profile:
  export ANDROID_HOME=$HOME/Library/Android/sdk
  export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0
  
  # Install build-tools using sdkmanager
  sdkmanager "build-tools;34.0.0"
  ```

  **Linux**:
  ```bash
  # Download command line tools
  wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
  unzip commandlinetools-linux-9477386_latest.zip
  
  # Set up directory structure
  mkdir -p ~/Android/sdk/cmdline-tools
  mv cmdline-tools ~/Android/sdk/cmdline-tools/latest
  
  # Add to PATH in ~/.bashrc:
  export ANDROID_HOME=$HOME/Android/sdk
  export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0
  
  # Install build-tools
  sdkmanager "build-tools;34.0.0"
  ```

  **Windows**:
  ```cmd
  # 1. Download commandline tools from Android Developer website
  # 2. Extract to C:\Android\sdk\
  # 3. Add to PATH:
  #    - C:\Android\sdk\cmdline-tools\latest\bin
  #    - C:\Android\sdk\platform-tools
  #    - C:\Android\sdk\build-tools\34.0.0
  
  # Install build-tools
  sdkmanager "build-tools;34.0.0"
  ```

### Verification

Test your installation by running:
```bash
java -version
apktool --version
aapt version        # or aapt2 version
dexdump -h
```

## Usage

### Basic Usage

```bash
# Without signing
./wire_apk_wrapper.sh <path_to_wire_apk>

# With signing
./wire_apk_wrapper.sh --sign --keystore <keystore_path> --key-alias <alias> <path_to_wire_apk>
```

### Examples

```bash
# Process a Wire APK without signing
./wire_apk_wrapper.sh wire.apk

# Process and sign a Wire APK
./wire_apk_wrapper.sh --sign --keystore my-release-key.jks --key-alias mykey wire.apk

# Process with signing and provide passwords via command line (less secure)
./wire_apk_wrapper.sh --sign --keystore my-release-key.jks --key-alias mykey --keystore-password mypass --key-password mypass wire.apk

# Process a Wire APK with full path
./wire_apk_wrapper.sh /path/to/downloads/wire-3.84.apk

# Show help
./wire_apk_wrapper.sh --help
```

### Command Line Options

- `-s, --sign`: Enable APK signing after wrapping
- `-k, --keystore PATH`: Path to the keystore file (required for signing)
- `-a, --key-alias ALIAS`: Key alias in the keystore (required for signing)
- `-p, --keystore-password`: Keystore password (will prompt if not provided)
- `-P, --key-password`: Key password (will prompt if not provided)
- `-h, --help`: Show help message

### Output

The script will create new APK files in the same directory as the input APK:

**Without signing:**
- Input: `wire.apk` → Output: `wire_wrapped.apk`
- Input: `wire-3.84.apk` → Output: `wire-3.84_wrapped.apk`

**With signing:**
- Input: `wire.apk` → Output: `wire_wrapped.apk` + `wire_wrapped_signed.apk`
- Input: `wire-3.84.apk` → Output: `wire-3.84_wrapped.apk` + `wire-3.84_wrapped_signed.apk`

The signed APK (`*_wrapped_signed.apk`) is ready for installation on Android devices.

## Script Features

- ✅ **Dependency checking**: Verifies Java, apktool, and signing tools are installed
- ✅ **Error handling**: Stops on errors and provides clear error messages
- ✅ **Progress tracking**: Shows current step and status
- ✅ **Cleanup**: Removes temporary work directories
- ✅ **File size comparison**: Shows original vs. wrapped vs. signed APK sizes
- ✅ **Colored output**: Easy to read status messages
- ✅ **APK signing**: Uses apksigner for secure APK signing
- ✅ **Secure password input**: Prompts for passwords without echoing to terminal
- ✅ **Signature verification**: Automatically verifies APK signatures after signing
- ✅ **APK integrity validation**:
  - DEX file count and size comparison
  - Class count verification (with dexdump)
  - Manifest comparison (with aapt)
  - File entry count validation
  - Critical component verification

## Important Notes

### APK Signing

#### Automatic Signing
The script can automatically sign your APK using the `--sign` option. This is the recommended approach as it handles the signing process correctly and verifies the signature.

#### Manual Signing (if needed)
If you need to sign the APK manually later, you can use:

```bash
# Using apksigner (Android SDK Build Tools)
apksigner sign --ks your-keystore.jks --ks-key-alias your-key-alias --out wire_wrapped_signed.apk wire_wrapped.apk
```

#### Creating a Keystore
If you don't have a keystore, you can create one:

```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mykey
```

### Troubleshooting

**Issue**: `apktool: command not found`
- **Solution**: Install apktool following the installation instructions above

**Issue**: `Java is not installed or not in PATH`
- **Solution**: Install Java JDK and ensure it's in your system PATH

**Issue**: `Failed to decompile APK`
- **Solution**: Ensure the APK file is valid and not corrupted
- Try using a different version of apktool

**Issue**: `com/wire directory not found in smali`
- **Solution**: The APK might already be processed or have a different structure
- Verify this is a genuine Wire APK

**Issue**: Permission denied when running script
- **Solution**: Make sure the script is executable: `chmod +x wire_apk_wrapper.sh`

**Issue**: `apksigner: command not found`
- **Solution**: Install Android SDK Build Tools (see installation instructions above)

**Issue**: `Key alias 'mykey' not found in keystore`
- **Solution**: List available aliases with `keytool -list -keystore your-keystore.jks` and use the correct alias

**Issue**: `Keystore file not found`
- **Solution**: Ensure the keystore path is correct and the file exists

**Issue**: `Failed to sign APK`
- **Solution**: Check that the keystore password and key password are correct

## Technical Details

### What the Script Does Internally

1. **Creates a temporary work directory** for decompilation
2. **Runs apktool decode** to decompile the APK into smali code
3. **Locates the com/wire directory** in the smali folder
4. **Creates smali_classes2 directory** if it doesn't exist
5. **Moves the entire com/wire folder** from smali to smali_classes2
6. **Runs apktool build** to recompile the modified APK
7. **Validates APK integrity** by comparing:
   - DEX file counts and sizes
   - Class counts (if dexdump available)
   - Manifest files (if aapt available)
   - File entry counts
8. **Optionally signs the APK** using apksigner
9. **Verifies the signature** if signing was performed
10. **Cleans up temporary files** and directories

### Why Move com/wire to smali_classes2?

This modification is typically done for:
- **Code organization**: Separating Wire-specific code from other app code
- **Reverse engineering**: Making it easier to analyze Wire's implementation
- **Custom modifications**: Preparing the APK for further modifications

## License

This script is provided as-is for educational and research purposes. Please ensure you comply with Wire's terms of service and applicable laws when using this tool.

## Contributing

If you encounter issues or have suggestions for improvements, please feel free to modify the script or create an issue report.
