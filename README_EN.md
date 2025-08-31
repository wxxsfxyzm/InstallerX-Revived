# InstallerX Revived (Community Edition)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

- This is a community-maintained fork after the [original project](https://github.com/iamr0s/InstallerX) was archived by the author
- Provides limited open-source updates and support
- Strictly follows GNU GPLv3 - all modifications are open source
- We welcome community contributions!

## Introduction

> A modern and functional Android app installer. (You know some birds are not meant to be caged, their feathers are just too bright.)

Looking for a better app installer? Try **InstallerX**!

Many customized Chinese ROMs come with subpar default installers. You can replace them with **InstallerX**.

Compared to stock installers, **InstallerX** offers more installation options:
- Rich installation types: APK, APKS, APKM, XAPK, APKs inside ZIP, and batch APKs.
- Dialog-based installation
- Notification-based installation
- Automatic installation
- Installer declaration
- Setting install flags (can inherit Profile settings)
- Dex2oat after success installation
- Block installation of specific apps
- Auto-delete APK after installation

## Supported Versions

- **Full support:** Android SDK 34 - 36 (Android 14 - 16)
- **Limited support:** Android SDK 24 - 33 (Android 7.0 - 13) (please report issues)

## Key Changes and Features

- **UI Options:** [Testing] Switchable between the classic interface and a new UI design based on Material 3 Expressive.
- **More Customization:** More customizable interface settings.
- **Bug fixes:** Resolved APK deletion issues from the original project on certain systems.
- **Performance:** Optimized parsing speed, improved parsing of various package types.
- **Multilingual support:** English, Traditional Chinese, and Spanish. Contributions for more languages are welcome via PR!
- **Dialog optimization:** Improved installation dialog display.
- **System Icons:** Support for displaying system icon packs during installation (method from [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku/blob/master/manager/src/main/java/moe/shizuku/manager/utils/AppIconCache.kt)).
- **Version Comparison:** Support for displaying version number comparison in single-line or multi-line format.
- **SDK Information:** Installation dialogs show targetSDK and minSDK in single-line or multi-line format.
- **Bypass Interceptions:** Shizuku/Root can bypass custom UI chain-start restrictions when opening an App after installation.
    - Implemented using native APIs, not shell commands.
    - Currently only works for dialog installation.
    - Dhizuku cannot invoke permissions, so a customizable countdown option was added to reserve time for the app opening action.
- **Extended Menu:** For dialog installation (enable in settings):
    - Displays permissions requested by the application.
    - InstallFlags configuration (can inherit global Profile settings). Partially based on code from [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions/blob/main/app/src/main/java/dev/zwander/installwithoptions/data/InstallOption.kt).
      - **Important Note:** Setting InstallFlags **does not guarantee** they will always work. Some options might pose security risks, depending on the system.
- **Preset Sources:** Support for pre-configuring installation source package names in settings, allowing quick selection in profiles and the dialog installation menu.
- **Install from ZIP:** Support for installing APK files inside ZIP archives (dialog installation only).
    - No quantity limit.
    - Supports APK files in nested directories within the ZIP, **not limited to the root directory**.
    - Supports automatic handling of multiple versions of the same package:
        - Deduplication.
        - Intelligent selection of the best package to install.
- **Batch Installation:** Support for installing multiple APKs at once (multi-select and share to InstallerX).
    - Dialog installation only.
    - No quantity limit.
    - APK files only.
    - Supports automatic handling of multiple versions of the same package (deduplication and intelligent selection).
- **APKS/APKM/XAPK Files:** Support for automatic selection of the best split. Partially based on ideas and code from [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller/tree/master/app).
    - Supports both notification and dialog installation.
        - Clicking "Install" in the notification chooses the best option.
        - In the dialog, the best option is selected by default, but can be chosen manually.
    - The split selection interface shows user-friendly descriptions.
- [Testing] **Architecture Support:** Allows installing armeabi-v7a packages on arm64-v8a only systems. Actual functionality depends on the system providing runtime translation.
- [Testing] **Downgrade with/without Data:** Support for performing app downgrades with or without data preservation on some OEM Android 15 systems.
    - This feature only supports Android 14 and above. On Android 14, try the `Allow downgrade` option in the installation settings first, and if it fails, use this suggestion.
    - The feature is available in the smart suggestions of the dialog installation. To use it, first enable the `Show intelligent suggestions (Experimental)` option.
    - **Use this feature with extreme caution on system apps!** Loss of system app data could render the device unusable.
    - Not compatible with OneUI 7.0, RealmeUI, and some ColorOS versions (OEM restrictions). If you only see the downgrade option *without* data preservation, it means your system does not support downgrade *with* data.
- [Testing] **Blacklist:** Support for configuring a list of banned package names for installation in the settings.
    - Under development. Currently only allows manual additions. A default list based on device model will be provided in the future (useful to prevent incorrect installation of system software from other models on HyperOS).
- [Testing] **DexOpt:** After successful installation, the app can automatically perform dex2oat on the installed applications according to the configured Profile settings.
- [Testing] **Directly Install From Download Link:** The online version supports directly sharing the download link of an APK file to InstallerX for installation. Currently, the APK is not kept locally, but an option to retain the installation package will be added in the future.

## FAQ

- **Dhizuku not working properly?**
    - Support for **official Dhizuku** is limited. Tested on AVDs with SDK ≥34. Operation on SDK <34 is not guaranteed.
    - When using `OwnDroid`, the `Auto delete after installation` function might not work correctly.
    - On Chinese ROMs, occasional errors are usually due to the system restricting Dhizuku's background operation. It is recommended to restart the Dhizuku app first.
    - Dhizuku has limited permissions. Many operations are not possible (like bypassing system intent interceptors or specifying the installation source). Using Shizuku is recommended if possible.

- **Lock tool not working?**
    - Due to the package name change, use the modified [InstallerX Lock Tool](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/main/InstallerX%E9%94%81%E5%AE%9A%E5%99%A8_1.3.apk) from this repository.

- **HyperOS shows "Installing system apps requires declaring a valid installer" error**
    - It's a system security restriction. You must declare an installer that is a system app (recommended: `com.android.fileexplorer` or `com.android.vending`).
    - Works with Shizuku/Root. **Dhizuku is not supported**.
    - New feature: InstallerX automatically detects HyperOS and adds a default configuration (`com.miui.packageinstaller`). You can change it in the settings if needed.

- **HyperOS reinstalls the default installer / locking fails**
    - HyperOS may reset the default installer after the user installs an APK-handling app.
    - On some HyperOS versions, locking failure is normal.
    - HyperOS intercepts USB installation requests (ADB/Shizuku) with a dialog. If the user rejects the installation of a new app, the system will revoke the installer setting and force the default one. If this happens, lock InstallerX again.

- **Notification progress bar freezes on HyperOS**
    - HyperOS has very strict background app controls. Set "No background restrictions" for the app.
    - The app is optimized: it ends all background services and closes 0.5 seconds after completing the installation task (when the user clicks "Done" or clears the notification). You can enable the foreground service notification to monitor.

- **Problems on Oppo/Vivo/Lenovo/... systems?**
    - We do not have devices from these brands for testing. You can discuss it in [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions).
    - To lock the installer on Oppo/Vivo, use the lock tool (Lock Tool).

## About Releases

> [!WARNING]
> Development versions may be unstable and features may change/be removed without notice.
> Switching build channels may require data wipe/reinstallation.

- **`dev` branch:** Contains features under development. If you want to test them, look for the corresponding builds in [Pull Requests](https://github.com/wxxsfxyzm/InstallerX-Revived/pulls).
  - Changes for each commit are detailed in the PRs (may be AI-generated).
- **`main` branch:** When stable changes are merged from `dev`, the CI/CD system automatically builds and publishes a new alpha version.
- **Stable releases:** Manually published when finishing a development phase and increasing the `versionCode`. CI/CD automatically publishes them as a release.
- About network permission: With feature expansion, some network-related functions have been introduced. However, many users prefer the installer to remain purely local without requiring network access. Therefore, two versions will be released: **online** and **offline**. Both versions share the same package name, version code, and signature, and can be installed side by side. Please download according to your needs.
  - **Online version**: Supports sharing direct download links to InstallerX for installation. More network-related utilities may be added in the future, but network permission will **never** be used for non-installation purposes. Safe to use.
  - **Offline version**: Requests no network permissions at all. When attempting to use online features, you will receive a clear error message. This version remains a purely local installer.

## License

Copyright © [iamr0s](https://github.com/iamr0s) and [contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX is currently released under [**GNU General Public License v3 (GPL-3)**](http://www.gnu.org/licenses/gpl-3.0), though this commitment may change in the future. Maintainers reserve the right to modify license terms or the open-source status of the project.

If you base your development on InstallerX, you must comply with the terms of the open-source license of the specific version of the source code you use as a base, regardless of future changes made to the main project.