#!/bin/bash

set -e

# === Configuration ===
APP_NAME="BilliardViewer"
MAIN_JAR="billiard-viewer.jar"
MAIN_CLASS="billiards.viewer.Main"
ICON_PATH="icon.icns"
BUILD_DIR="build"
INPUT_DIR="${BUILD_DIR}/libs"
DIST_DIR="dist"
RUNTIME_IMAGE="${BUILD_DIR}/custom-runtime"
DYLIB_TEMP_DIR="${BUILD_DIR}/javafx-natives"

# === Step 1: Auto-detect JAVA_HOME for JDK 17 ===
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
#export JAVA_HOME=$(/usr/libexec/java_home -v 17)
echo "[INFO] Detected JAVA_HOME: $JAVA_HOME"

echo "[INFO] Ensuring main JAR is present..."
cp -f "$INPUT_DIR/$MAIN_JAR" "$BUILD_DIR/$MAIN_JAR"


# === Step 5.6: Ensure native dylib is present ===
echo "[INFO] Copying libbackend.dylib to input/backend/shared"
cp -f "build/libs/backend/shared/libbackend.dylib" "$BUILD_DIR/"

# === Step 2: Resolve JavaFX native libraries from Gradle ===
echo "[INFO] Resolving JavaFX runtime JARs..."
./gradlew copyRuntimeLibs -q

# === Step 3: Extract native .dylib files from JavaFX jars ===
echo "[INFO] Extracting JavaFX native libraries from jars..."

mkdir -p "$DYLIB_TEMP_DIR"
rm -rf "$DYLIB_TEMP_DIR"/*
find "$INPUT_DIR" -name "javafx-*.jar" | while read -r jar; do
  unzip -q "$jar" "*.dylib" -d "$DYLIB_TEMP_DIR" || true
done

echo "[INFO] Creating custom Java runtime with jlink..."
"$JAVA_HOME/bin/jlink" \
  --module-path "$JAVA_HOME/jmods:$(./gradlew -q runtimeClasspathAsPath)" \
  --add-modules java.base,java.logging,java.desktop,javafx.controls,javafx.fxml,javafx.graphics,java.sql,jdk.crypto.ec,java.security.jgss \
  --output "$RUNTIME_IMAGE" \
  --strip-debug \
  --compress=2 \
  --no-header-files \
  --no-man-pages

# === Step 5: Copy .dylib files to custom runtime ===
echo "[INFO] Copying native .dylib files into runtime..."
cp "$DYLIB_TEMP_DIR"/*.dylib "$RUNTIME_IMAGE/bin" || echo "[WARN] No dylibs found to copy"

echo "[INFO] Ensuring main JAR is present..."
cp -f "$BUILD_DIR/$MAIN_JAR" "$INPUT_DIR/$MAIN_JAR"

# === Step 5.6: Ensure native dylib is present ===
# === Step 5.6: Bundle and Patch Native Libraries ===
NATIVE_DIR="$INPUT_DIR/backend/shared"
mkdir -p "$NATIVE_DIR"

echo "[INFO] Copying libbackend.dylib..."
#cp "$BUILD_DIR/libs/backend/shared/libbackend.dylib" "$NATIVE_DIR/"
cp "$BUILD_DIR/libbackend.dylib" "$NATIVE_DIR/"
# 1. Define the Homebrew libraries we depend on
# NOTE: Using 'readlink' to ensure we get the actual file, not just the symlink
DEPENDENCIES=(
    "/opt/homebrew/opt/gmp/lib/libgmp.10.dylib"
    "/opt/homebrew/opt/mpfr/lib/libmpfr.6.dylib"
    "/opt/homebrew/opt/mpfi/lib/libmpfi.0.dylib"
    "/opt/homebrew/opt/tbb/lib/libtbb.12.dylib"
    "/opt/homebrew/opt/boost/lib/libboost_thread.dylib"
    "/opt/homebrew/opt/boost/lib/libboost_system.dylib"
)

# 2. Copy dependencies into the app bundle
echo "[INFO] Bundling dependencies..."
for dep in "${DEPENDENCIES[@]}"; do
    if [ -f "$dep" ]; then
        echo "  -> Bundling $(basename "$dep")"
        cp "$dep" "$NATIVE_DIR/"
        # Ensure it's writable so we can patch it
        chmod 755 "$NATIVE_DIR/$(basename "$dep")"
    else
        echo "[ERROR] Could not find dependency: $dep"
        echo "Please ensure you have installed: brew install gmp mpfr mpfi tbb boost"
        exit 1
    fi
done

# 3. Patching the libraries (The Magic Step)
# This changes absolute paths (/opt/homebrew/...) to relative paths (@loader_path/...)
echo "[INFO] Patching library paths (install_name_tool)..."

cd "$NATIVE_DIR"

# A. Helper function to change the ID of a library to be relative
fix_dylib_id() {
    local lib_name=$1
    echo "  -> Setting ID for $lib_name"
    install_name_tool -id "@loader_path/$lib_name" "$lib_name"
}

# B. Helper function to fix dependency references inside a library
fix_dependencies() {
    local target_lib=$1
    # Check for references to Homebrew libs and rewrite them to @loader_path
    otool -L "$target_lib" | grep "/opt/homebrew" | awk '{print $1}' | while read -r dep_path; do
        local dep_name=$(basename "$dep_path")
        echo "     Fixing ref in $target_lib: $dep_name"
        install_name_tool -change "$dep_path" "@loader_path/$dep_name" "$target_lib"
    done
}

# Apply fixes to ALL .dylib files in the directory (backend + dependencies)
for dylib in *.dylib; do
    fix_dylib_id "$dylib"
    fix_dependencies "$dylib"
done

# Return to build root
cd - > /dev/null

# ... other copy commands
echo "[INFO] Copying updater.sh to input directory..."
cp -f "updater.sh" "$INPUT_DIR/"
chmod +x "$INPUT_DIR/updater.sh"

cp "updater.bat" "$INPUT_DIR/"

# === Step 6: Package with jpackage ===
echo "[INFO] Running jpackage..."
"$JAVA_HOME/bin/jpackage" \
  --type dmg \
  --name "$APP_NAME" \
  --input "$INPUT_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --runtime-image "$RUNTIME_IMAGE" \
  --dest "$DIST_DIR" \
  --icon "$ICON_PATH" \
  --java-options "-Djna.library.path=\$APPDIR/backend/shared" \
  --java-options "--add-modules=javafx.controls,javafx.fxml,java.sql" \
  --app-version 2.3


echo "[✅ SUCCESS] Installer created at $DIST_DIR"
