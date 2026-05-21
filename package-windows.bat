@echo off
setlocal

rem === Configuration ===
set APP_NAME="BilliardViewer"
set MAIN_JAR="billiard-viewer.jar"
set MAIN_CLASS="billiards.viewer.Main"
set ICON_PATH="icon.ico"
set BUILD_DIR="build"
set INPUT_DIR="%BUILD_DIR%\libs"
set DIST_DIR="dist"
set RUNTIME_IMAGE="%BUILD_DIR%\custom-runtime"
set DYLIB_TEMP_DIR="%BUILD_DIR%\javafx-natives"

rem === Step 1: Auto-detect JAVA_HOME ===
rem Assumes JDK 17 is on the system PATH
for /f "tokens=*" %%i in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr "java.home"') do set JAVA_HOME=%%i
set JAVA_HOME=%JAVA_HOME:java.home =%
set JAVA_HOME=%JAVA_HOME:~1%
echo [INFO] Detected JAVA_HOME: %JAVA_HOME%

rem === Step 2: Ensure main JAR is present ===
echo [INFO] Ensuring main JAR is present...
copy /y "%INPUT_DIR%\%MAIN_JAR%" "%BUILD_DIR%\%MAIN_JAR%"

rem === Step 3: Resolve JavaFX native libraries from Gradle ===
echo [INFO] Resolving JavaFX runtime JARs...
call gradlew.bat copyRuntimeLibs -q

rem === Step 4: jlink with JavaFX modules ===
echo [INFO] Creating custom Java runtime with jlink...
"%JAVA_HOME%\bin\jlink" ^
  --module-path "%JAVA_HOME%\jmods;$(call gradlew.bat -q runtimeClasspathAsPath)" ^
  --add-modules java.base,java.logging,java.desktop,javafx.controls,javafx.fxml,javafx.graphics,java.sql,jdk.crypto.ec,java.security.jgss ^
  --output "%RUNTIME_IMAGE%" ^
  --strip-debug ^
  --compress=2 ^
  --no-header-files ^
  --no-man-pages

rem === Step 5: Copy updater.bat and libbackend.dll ===
echo [INFO] Copying updater.bat to input directory...
copy /y "updater.bat" "%INPUT_DIR%"
echo [INFO] Copying libbackend.dll to input/backend/shared...
mkdir "%INPUT_DIR%\backend\shared"
copy /y "%BUILD_DIR%\libs\backend\shared\libbackend.dll" "%INPUT_DIR%\backend\shared\"

rem === Step 6: Package with jpackage ===
echo [INFO] Running jpackage...
"%JAVA_HOME%\bin\jpackage" ^
  --type msi ^
  --name %APP_NAME% ^
  --input "%INPUT_DIR%" ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --runtime-image "%RUNTIME_IMAGE%" ^
  --dest "%DIST_DIR%" ^
  --icon "%ICON_PATH%" ^
  --win-menu ^
  --win-dir-chooser ^
  --java-options "-Djna.library.path=\$APPDIR/backend/shared" ^
  --java-options "--add-modules=javafx.controls,javafx.fxml,java.sql" ^
  --app-version 2.1

echo [SUCCESS] Installer created at %DIST_DIR%
endlocal