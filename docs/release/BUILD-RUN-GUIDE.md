# Build And Run Guide

This guide is for the Abdul Windows source tree. It assumes the user can run a terminal from the project root.

Project root:

```text
-Abdul-s-fork-BilliardsEverything
```

## What Gradle Builds

The normal Gradle run path builds both sides:

- Java/JavaFX frontend from `src/java`
- C++ native backend from `src/backend`
- `backend.dll`, `libbackend.dylib`, or Linux shared library under `build/libs/backend/shared`
- Java app launch with `jna.library.path` pointing at that backend output

Run does both build and launch:

```powershell
.\gradlew.bat --no-daemon run
```

On macOS/Linux:

```bash
./gradlew --no-daemon run
```

## Java Requirement

Use Java 17. The Gradle file sets:

```groovy
sourceCompatibility = JavaVersion.VERSION_17
targetCompatibility = JavaVersion.VERSION_17
```

On Windows, check:

```powershell
java -version
$env:JAVA_HOME
```

If needed for one shell only:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.x"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

This is temporary for that PowerShell session.

## Windows Setup

Use MSYS2 UCRT64 unless there is a specific reason not to. Install or update MSYS2 first, then install dependencies.

Update MSYS2:

```powershell
C:\msys64\usr\bin\pacman.exe -Syu
```

If pacman updates core MSYS2 packages and asks you to close the terminal, close it, open a new PowerShell, and run the same command again until it says there is nothing to do.

Install project dependencies:

```powershell
C:\msys64\usr\bin\pacman.exe -S --needed `
    mingw-w64-ucrt-x86_64-gcc `
    mingw-w64-ucrt-x86_64-boost `
    mingw-w64-ucrt-x86_64-eigen3 `
    mingw-w64-ucrt-x86_64-tbb `
    mingw-w64-ucrt-x86_64-gmp `
    mingw-w64-ucrt-x86_64-mpfr `
    mingw-w64-ucrt-x86_64-mpfi `
    mingw-w64-ucrt-x86_64-sqlite3 `
    mingw-w64-ucrt-x86_64-gdb `
    mingw-w64-ucrt-x86_64-pkgconf `
    mingw-w64-ucrt-x86_64-make
```

Optional, only if MSYS2 is not under `C:\msys64`:

```powershell
$env:MSYS2_ROOT = "D:\msys64"
$env:BILLIARDS_MSYS2_PREFIX = "D:\msys64\ucrt64"
```

Build and launch:

```powershell
.\gradlew.bat --no-daemon clean run
```

Run with a worker-thread limit:

```powershell
.\gradlew.bat --no-daemon run --args="--threads=2"
```

The app defaults to half of detected logical processors. If `--threads=N` is supplied, Java clamps it to `1..availableProcessors-1` so the OS and JavaFX event thread have CPU headroom. On a 1-core machine, the maximum remains 1.

The same resolved thread count is pushed into the native backend through `backend_set_worker_threads`.

## Windows Validation Commands

Compile Java and native backend:

```powershell
.\gradlew.bat --no-daemon compileJava backendSharedLibrary
```

Run native Boost tests:

```powershell
.\gradlew.bat --no-daemon testBackend
```

Run Java tests:

```powershell
.\gradlew.bat --no-daemon test
```

Recommended release smoke test:

```powershell
.\gradlew.bat --no-daemon compileJava backendSharedLibrary testBackend test
```

## Windows Debug Builds

Build native backend with debug symbols:

```powershell
.\gradlew.bat --no-daemon -PbilliardsNativeDebug=true clean backendSharedLibrary
```

Launch with Java debugging:

```powershell
.\gradlew.bat --no-daemon run --debug-jvm --args="--threads=2"
```

Attach an IDE debugger to port `5005`.

For native debugging, launch the app, then attach MSYS2 GDB to the Java process:

```powershell
C:\msys64\ucrt64\bin\gdb.exe -p <java_pid>
```

Useful native breakpoints:

```gdb
break backend_set_worker_threads
break load_picture
break cover_wrapper
break verify
break check_cover
break vary_4_cpp
```

## Windows Process Watcher

`docs/procwatch.ps1` is a lightweight PowerShell monitor for the running Billiards Java process. Start the app first, then run this from the project root in another PowerShell:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\docs\procwatch.ps1
```

The script auto-detects the newest `java.exe` or `javaw.exe` process whose command line contains general Billiards markers such as `billiards.viewer.Main`, `billiard-viewer`, `BilliardsEverything`, or `Billards_Stable`. It prints CPU time, private memory, virtual memory, working set, and thread count once per second.

Use this while running large OSNO, cover, vary, or cancel/resume tests to see whether memory plateaus after warmup or keeps growing. This is a quick diagnostic tool, not a full profiler; use VMMap, Java Flight Recorder, VisualVM, or GDB when the numbers suggest a deeper leak.

## Runtime Memory Knobs

Current defaults are intentionally large enough for this application:

```text
Xms = 2g
Xmx = 6g
MaxDirectMemorySize = 2g
```

Temporary Gradle property override:

```powershell
.\gradlew.bat --no-daemon -PbilliardsXmx=12g run --args="--threads=4"
```

Temporary environment override example:

```powershell
$env:BILLIARDS_XMX = "12g"
.\gradlew.bat --no-daemon run --args="--threads=4"
```

Do not set machine-wide Java options for this. Keep changes shell-local or Gradle-command-local.

## Compile Resource Limits

`gradle.properties` limits build pressure:

```properties
org.gradle.workers.max=2
org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
org.gradle.parallel=false
```

These settings affect Gradle compilation, not the launched app heap.

## macOS Setup

Install dependencies with Homebrew:

```bash
brew install openjdk@17 boost eigen tbb gmp mpfr mpfi sqlite
```

Set Java for the shell:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export PATH="$JAVA_HOME/bin:$PATH"
```

Build and run:

```bash
./gradlew --no-daemon clean run --args="--threads=4"
```

The Gradle file links Homebrew native libraries from `/opt/homebrew` on Apple Silicon. Intel Homebrew users may need to add `/usr/local` library/include paths if their Homebrew prefix differs.

## Linux Setup

On Ubuntu/Debian-like systems:

```bash
sudo apt update
sudo apt install openjdk-17-jdk g++ libboost-all-dev libeigen3-dev libtbb-dev libgmp-dev libmpfr-dev libmpfi-dev libsqlite3-dev openjfx
```

Build and run:

```bash
./gradlew --no-daemon clean run --args="--threads=4"
```

The current Gradle file expects JavaFX modules under `/usr/share/openjfx/lib` for Linux compile/run. If your distribution installs JavaFX elsewhere, update the module path in `build.gradle` or install the distro OpenJFX package that provides that path.

## Common Failures

`cannot find -lboost_thread` or `cannot find -lboost_system` on Windows:

- You are probably mixing MSYS2 environments.
- Use UCRT64 packages and let Gradle choose `C:/msys64/ucrt64`.
- Confirm `C:\msys64\ucrt64\lib` contains Boost libraries.

`undefined reference to WSAStartup`:

- Windows backend must link `-lws2_32`.
- This is already fixed in the Abdul Windows Gradle file.

`cannot find -ltbb` or `cannot find -ltbb12`:

- Windows UCRT64 uses `tbb12`.
- Native tests and backend are aligned to `-ltbb12` on Windows.

Java starts but cannot load backend:

- Build `backendSharedLibrary`.
- Run through Gradle so `JNA_LIBRARY_PATH`, `PATH`, `LD_LIBRARY_PATH`, or `DYLD_LIBRARY_PATH` are set.
- On Windows, verify `build/libs/backend/shared/backend.dll` exists.

IDE include errors for Boost/Eigen:

- Point the IDE C++ indexer at `C:\msys64\ucrt64\include` and `C:\msys64\ucrt64\include\eigen3`.
- Configure the IDE to use the Gradle/native compile commands or the UCRT64 GCC toolchain.
- Do not index against the old Makefile unless you also update its include paths.
