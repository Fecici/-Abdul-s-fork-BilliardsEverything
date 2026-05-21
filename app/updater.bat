@echo off
set oldjar=%1
set newjar_zip=%2

rem Wait for 2 seconds to allow the Java process to exit
timeout /t 2 /nobreak

rem Get the directory of the app bundle (the 'app' folder)
for %%a in ("%oldjar%") do set app_dir=%%~dpa

rem Delete old files and folders from the app directory, but be careful
rem not to delete the updater.bat script itself
del /f /q "%app_dir%\*"
for /d %%d in ("%app_dir%\*") do rmdir /s /q "%%d"

rem Unzip the new files directly into the app directory
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%newjar_zip%' -DestinationPath '%app_dir%' -Force"

rem Cleanup the downloaded zip file
del /f "%newjar_zip%"

echo Update complete. Please restart the application.