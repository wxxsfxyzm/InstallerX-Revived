# InstallerX Revived (Community Edition)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)

- This is a community-maintained fork after the [original project](https://github.com/iamr0s/InstallerX) was archived by the author
- Provides limited open-source updates and support
- Strictly follows GNU GPLv3 - all modifications are open source

## Introduction

> A modern and functional Android app installer. (You know some birds are not meant to be caged, their feathers are just too bright.)

Looking for a better app installer? Try **InstallerX**!

Many customized Chinese ROMs come with subpar default installers. You can replace them with **InstallerX**.

Compared to stock installers, **InstallerX** offers more installation options:
- Dialog-based installation
- Notification-based installation
- Automatic installation
- Installer declaration
- Option to install for all users
- Allow test packages
- Allow downgrade installation
- Auto-delete APK after installation

## Supported Versions

- **Full support:** Android SDK 34 - 36
- **Limited support:** Android SDK 30 - 33 (please report issues)

## Key Changes

- **UI Options:** Switch between the classic UI style and a new Material 3 Expressive design [Experimental]
- **Customization:** More configurable UI settings
- **Bug fixes:** Resolved APK deletion issues from the original project on certain systems
- **Performance:** Improved parsing speed and compatibility with more package types
- **Multilingual support:** English, Traditional Chinese, and Spanish. Contributions for more languages welcome!
- **Dialog optimization:** Improved installation dialog display
- **New features:**
  - Single-line / multi-line version comparison display
  - Shows targetSDK and minSDK during installation
  - Displays system icon packs (method from [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku))
  - Shizuku/Root can bypass chained launch interception on customized systems (dialog installation only)
    - Implemented via native API, no shell commands
    - Dhizuku lacks permission, so a countdown option is provided to allow opening apps in time
  - Extended installation dialog menu (enable in settings):
    - Displays requested permissions by the app to install
    - InstallFlags configuration (inherits global Profile settings, based on [InstallWithOptions](https://github.com/zacharee/InstallWithOptions))
      - **Note:** Effectiveness depends on the system, and some options may pose security risks
  - Preset installation source package names in settings, selectable in config and dialog menu
  - Supports installing APKs inside ZIP archives using InstallerX:
    - Dialog installation only
    - No file count limits; supports nested directories (not limited to root directory of ZIP)
    - Automatically handles multiple versions of the same package
      - Supports deduplication
      - Intelligent selection of the best APK
  - Batch installation (multi-select and share to InstallerX):
    - Dialog installation only
    - No file count limits
    - APK files only
    - Automatically handles multiple versions of the same package
      - Supports deduplication
      - Intelligent selection of the best APK
  - APKS/APKM/XAPK split package auto-selection (inspired by [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller/tree/master/app))
    - Works in both notification and dialog installation
      - Notification installation always chooses the best option
      - Dialog defaults to best option but allows manual override
    - Split selection UI shows user-friendly descriptions
  - [Experimental] Install armeabi-v7a packages on arm64-v8a only systems (runtime compatibility depends on system)
  - [Experimental] Data-preserving and non-data-preserving downgrade installation on some OEM Android 14/15 systems
    - Requires Android 14+. On Android 14, first try the built-in `Allow downgrade` install option, then this feature if it fails
    - Integrated into "Smart Suggestions (Experimental)" in dialog installation
    - Do **not** use on system apps — may cause data loss and render the system unusable
    - Not supported on OneUI 7.0, RealmeUI, or some ColorOS (OEM-limited) — only non-data-preserving downgrade will appear if unsupported
  - [Experimental] Support for configuring a blocklist of package names in settings. Apps listed there will be denied during installation.
    - Currently under development — only manual entry is supported for now.  
      In the future, default blocklists will be provided based on device manufacturers
      (especially useful on HyperOS to prevent accidental installation of system apps from other devices)
## FAQ

- **Dhizuku not working properly**
  - Only minimal support for **official Dhizuku**
  - Tested on SDK ≥34 AVDs; not guaranteed below SDK 34
  - `OwnDroid` may break "auto-delete after install"
  - On some Chinese ROMs, Dhizuku is restricted in background. Restart the app and try again if encountering issues
  - Dhizuku has insufficient permissions (cannot bypass intent interception, set installer source, etc.). Therefore Shizuku is recommended

- **Lock tool not working**
  - Due to package name changes, use the modified [InstallerX Lock Tool](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/main/InstallerX%E9%94%81%E5%AE%9A%E5%99%A8_1.3.apk)

- **HyperOS shows "System app installation requires valid installer declaration" error**
  - System security restriction
  - Declare a system installer (recommended: `com.android.fileexplorer` or `com.android.vending`)
  - Works with Shizuku/Root (Dhizuku not supported)
  - App auto-adds `com.miui.packageinstaller` to config on HyperOS startup; change it in settings if needed

- **HyperOS reverts to default installer**
  - HyperOS may occasionally revoke the user’s installer setting for unknown reasons
  - On some HyperOS versions, being unable to lock the installer is expected
  - HyperOS revokes permissions if user rejects ADB/Shizuku installation
  - Solution: Manually relock the installer

- **Installation notification freezes on HyperOS**
  - HyperOS has strict background app controls
  - Enable "No background restrictions"
  - App automatically closes 0.5 seconds after completing installation

- **Issues on Oppo/Vivo/Lenovo devices**
  - We don't have these devices for testing. Discuss solutions in [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions)
  - For Oppo/Vivo devices, use the Lock Tool to set InstallerX may be always required

## About Releases

> [!WARNING]
> Development versions may be unstable and features may change/be removed without notice.
> Switching build channels may require data wipe/reinstallation.

- **`dev` branch:** Features in testing (find builds in [Pull Requests](https://github.com/wxxsfxyzm/InstallerX-Revived/pulls))
  - Changes documented in PRs (may be AI-generated)
- **Alpha versions:** Automatic when merged to `main`
- **Stable releases:** Manually published when incrementing `versionCode`

## License

Copyright © [iamr0s](https://github.com/iamr0s) and [contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX is currently released under [**GNU GPL v3**](http://www.gnu.org/copyleft/gpl.html), though this commitment may change in the future. Maintainers reserve the right to modify license terms or close the source.

If you create derivative works based on InstallerX, you must comply with the GPL-3.0 terms of the specific version you use as base, regardless of future changes to the main project.
