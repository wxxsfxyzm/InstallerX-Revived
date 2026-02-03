#!/system/bin/sh
MODDIR=${0%/*}
PKG=$(cat $MODDIR/package.txt)
am start -n $PKG/com.rosan.installer.ui.activity.SettingsActivity
