#!/system/bin/sh
SKIPUNZIP=1

# Define function to unpack zip file
unpack_zip() {
    unzip -o "$1" "$2" -d "$3"
}

# Define function to get user confirmation before proceeding
confirm_to_proceed() {
    SECONDS_WAIT=10
    while true; do
        event=$(timeout "$SECONDS_WAIT" getevent -ql | grep -m 1 -E "KEY_VOLUMEUP|KEY_VOLUMEDOWN")
        
        if [ -z "$event" ]; then
            echo "No volume key detected; proceeding by default..."
            return 0
        elif echo "$event" | grep -q "KEY_VOLUMEUP"; then
            echo "Volume Up detected, proceeding..."
            return 0
        elif echo "$event" | grep -q "KEY_VOLUMEDOWN"; then
            echo "Volume Down detected, this operation will be cancelled..."
            return 1
        fi
    done
}

# Unpack necessary files from the zip archive
unpack_zip "$ZIPFILE" "package.txt" "$MODDIR"
unpack_zip "$ZIPFILE" "module.prop" "$MODDIR"
unpack_zip "$ZIPFILE" "service.sh" "$MODDIR"
unpack_zip "$ZIPFILE" "uninstall.sh" "$MODDIR"
unpack_zip "$ZIPFILE" "action.sh" "$MODDIR"

# Get package name and verify
PACKAGE_NAME=$(cat "$MODDIR/package.txt")
PACKAGE_VERIFY=$(pm list packages | grep -oE "$PACKAGE_NAME" | awk -F ':' '{print $2}' | grep -vE '([A-Z]|overlay)' | sort -n | head -n 1)
if [ "$PACKAGE_NAME" != "$PACKAGE_VERIFY" ]; then
    echo "Package not found, are you sure this is the correct module?"
    exit 1
fi

# Get package path and check if it's in the system path
PACKAGE_PATH=$(pm path "$PACKAGE_NAME" | cut -d':' -f2)
IS_SYSTEM_PATH=false
case "$PACKAGE_PATH" in
    /system/*|/product/*|/system_ext/*|/vendor/*|/mnt/product/*)
        [ "$(echo "$PACKAGE_PATH" | cut -c1-6)" != "/data/" ] && IS_SYSTEM_PATH=true
        ;;
esac

# If the package is in the system path, proceed with uninstallation and setup
if "$IS_SYSTEM_PATH"; then
    TARGET_DIR=$(dirname "$PACKAGE_PATH")
    mkdir -p "$MODDIR/system$TARGET_DIR"
    unpack_zip "$ZIPFILE" "InstallerX_Revived.apk" "$MOD_DIR/system$TARGET_DIR"
    mv "$MODDIR/system$TARGET_DIR/InstallerX_Revived.apk" "$MOD_DIR/system$PACKAGE_PATH"
else
    echo "The package is not in system path, please check the path: $PACKAGE_PATH"
    exit 1
fi

# Set permissions recursively for the module path
set_perm_recursive "$MODDIR" 0 0 755 644
