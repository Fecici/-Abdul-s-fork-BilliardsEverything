#!/bin/bash
oldjar="$1"
newjar_zip="$2"

sleep 2

# Get the directory of the app bundle (the 'app' folder)
app_dir=$(dirname "$oldjar")

# Create a temporary directory for unzipping
temp_dir=$(mktemp -d)

# Unzip the new files into a temporary directory
unzip -q "$newjar_zip" -d "$temp_dir"

# Find the actual app folder inside the unzipped content
app_content_dir="$temp_dir/app"
if [ ! -d "$app_content_dir" ]; then
    echo "Could not find 'app' directory inside the zip file."
    exit 1
fi

# Move the unzipped files to the application's app directory, overwriting old files
rsync -a --delete "$app_content_dir/" "$app_dir/"

# Cleanup
rm -rf "$temp_dir"
rm -f "$newjar_zip"

echo "Update complete. Please restart the application."