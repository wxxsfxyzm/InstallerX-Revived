# InstallerX Revived (Community Edition)

**English** | [简体中文](README_CN.md) | [Español](README_ES.md) | [日本語](README_JA.md) | [Deutsch](README_DE.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

- This is a community-maintained fork after the [original project](https://github.com/iamr0s/InstallerX) was archived by the author
- Provides limited open-source updates and support
- Strictly follows GNU GPLv3 - all modifications are open source
- We welcome community contributions!

## Introduction

> A modern and functional Android app installer. (You know some birds are not meant to be caged, their feathers are just too bright.)

Looking for a better app installer? Try **InstallerX**!

Many customized Chinese ROMs come with subpar default installers. You can replace them with **InstallerX Revived**.

Compared to stock installers, **InstallerX Revived** offers more installation features:
- Rich installation types: APK, APKS, APKM, XAPK, APKs inside ZIP, and batch APKs.
- Dialog-based installation
- Notification-based installation (Live Activity API supported)
- Automatic installation
- Installer declaration
- Setting install flags (can inherit Profile settings)
- Install for specific user / all users
- Dex2oat after successful installation
- Block the installation of specific app's packageName or by sharedUID
- Auto-delete APK after installation
- No shell commands, native API calls only

## Supported Versions

- **Full support:** Android SDK 34 - 36.1 (Android 14 - 16)
- **Limited support:** Android SDK 26 - 33 (Android 8.0 - 13) (please report issues)

## Key Changes and Features

- **UI Options:** Switchable between a new UI design based on Material 3 Expressive and Miuix which is like HyperOS.
- **More Customization:** More customizable interface settings.
- **Bug fixes:** Resolved APK deletion issues from the original project on certain systems.
- **Performance:** Optimized parsing speed, improved parsing of various package types.
- **Multilingual support:** More languages supported. Contributions for more languages are welcome!
- **Dialog optimization:** Improved installation dialog display.
- **System Icons:** Support for displaying system icon packs during installation. Allows switching between APK icons and system icon packs through a toggle.
- **Version Comparison:** Support for displaying version number comparison in single-line or multi-line format.
- **SDK Information:** Installation dialogs show targetSDK and minSDK in single-line or multi-line format.
- **Session Install Confirmation**: With the help of [InxLocker](https://github.com/Chimioo/InxLocker), confirming installations from store apps (Aurora Store, F-Droid, etc.) is now supported.
- **Bypass Interceptions:** Shizuku/Root can bypass custom OS chain-start restrictions when opening an App after installation.
    - Currently only works for dialog installation.
    - Dhizuku lacks sufficient permissions, so a customizable countdown option was added to reserve time for the app opening action.
- **Extended Menu:** For dialog installation (can be enabled in settings):
    - Displays permissions requested by the application.
    - InstallFlags configuration (can inherit global Profile settings).
      - **Important:** Setting InstallFlags **does not guarantee** they will always work. Some options might pose security risks, depending on the system.
- **Preset Sources:** Support for pre-configuring installation source package names in settings, allowing quick selection in profiles and the dialog installation menu.
- **Install from ZIP:** Support for installing APK files inside ZIP archives (dialog installation only).
    - Supports unlimited quantity and multiple ZIP files.
    - Supports APK files in nested directories within the ZIP, **not limited to the root directory**.
    - Supports automatic handling of multiple versions of the same package:
        - Deduplication
        - Smart selection of the best package to install.
- **Batch Installation:** Support for installing multiple APKs at once (multi-select and share to InstallerX).
    - Dialog installation only.
    - No quantity limit.
    - APK files only.
    - Supports automatic handling of multiple versions of the same package (deduplication and smart selection).
- **APKS/APKM/XAPK Files:** Support for automatic selection of the best split.
    - Supports both notification and dialog installation.
        - Clicking "Install" in the notification selects the best option and proceeds with installation.
        - In the dialog, the best option is selected by default, but can be chosen manually.
    - The split selection interface shows user-friendly descriptions.
- **Architecture Support:** Allows installing armeabi-v7a packages on arm64-v8a only systems (actual functionality depends on the system providing runtime translation).
- **Downgrade with or without Data:** Support for performing app downgrades with or without data preservation on some OEM Android 15 systems.
    - This feature only supports Android 15. On Android 14 or below, try the `Allow downgrade` option in the install options.
    - The feature is available in the smart suggestions of the dialog installation. To use it, first enable the `Show smart suggestions` option.
    - **Use this feature with extreme caution on system apps!** Loss of data from a system app could render the device unusable.
    - Not compatible with OneUI 7.0, RealmeUI, and some ColorOS versions (AOSP has fixed). If you only see the downgrade option *without* data preservation, it means your system does not support downgrade *with* data.
- **Blacklist:** Support for configuring a list of banned package names for installation in the settings.
    - Support blacklist by packageName / sharedUID with exemptions
    - `Allow once` in smart suggestions
- **DexOpt:** After successful installation, the app can automatically perform dex2oat on the installed applications according to the configured Profile settings.
    - Does not support Dhizuku
- **Signature Verification：** Verify the signature of the installed app and apk to install, and give a warning if they do not match.
- **Select Target User:** Support installing apps to a specific user.
    - Dynamically obtain current user details.
    - Does not support Dhizuku
    - Can be overridden by `Install For All Users` install option
- **Declare as Uninstaller:** Accept Uninstall intent on certain OS, custom OS may not be supported.
- [Experimental] **Directly Install From Download Link:** The online version supports directly sharing the download link of an APK file to InstallerX for installation. Currently, the APK is not kept locally, but an option to retain the installation package will be added in the future.

## FAQ

> [!NOTE]
> Please read the FAQ before providing feedback.
> When providing feedback, please specify your phone brand, system version, software version, and operation in detail.

- **Dhizuku not working properly?**
    - Support for **official Dhizuku** is limited. Tested on AVDs with SDK ≥34. Operation on SDK <34 is not guaranteed.
    - When using `OwnDroid`, the `Auto delete after installation` function might not work correctly.
    - On Chinese ROMs, occasional errors are usually due to the system restricting Dhizuku's background operation. It is recommended to restart the Dhizuku app first.
    - Dhizuku has limited permissions. Many operations are not possible (like bypassing system intent interceptors or specifying the installation source). Using Shizuku is recommended if possible.

- **Unable to lock InstallerX as default installer?**
    - Some Systems have very strict policy on Package Installers. You must use a LSPosed module to intercept the intent and forward it to the installer in this case.
    - Works best with: [Chimioo/InxLocker](https://github.com/Chimioo/InxLocker)
    - Other lockers working as LSPosed are no longer recommended

- An error occurred in the resolution phase: `No Content Provider` or `reading provider` reported `Permission Denial`?
    - You have enabled Hide app list or similar functions, please configure the whitelist.

- **HyperOS shows "Installing system apps requires declaring a valid installer" error**
    - It's a system security restriction. You must declare an installer that is a system app (recommended: `com.android.fileexplorer` or `com.android.vending` for HyperOS; app store for Vivo).
    - Works with Shizuku/Root. **Dhizuku is not supported**.
    - New feature: InstallerX automatically detects HyperOS and adds a default configuration (`com.miui.packageinstaller`). You can change it in the settings if needed.

- **HyperOS reinstalls the default installer / locking fails**
    - Try enabling `Auto Lock Installer` in settings.
    - On some HyperOS versions, locking failure is expected.
    - HyperOS intercepts USB installation requests (ADB/Shizuku) with a dialog. If the user rejects the installation of a new app, the system will revoke the installer setting and force the default one. If this happens, lock InstallerX again.

- **Notification progress bar freezes**
    - Some custom OS has very strict background app controls. Set "No background restrictions" for the app if you encounter this.
    - The app is optimized: it ends all background services and closes 1 second after completing the installation task (when the user clicks "Done" or clears the notification). You can enable the foreground service notification to monitor.

- **Problems on Oppo/Vivo/Lenovo/... systems?**
    - We do not have devices from these brands for testing. You can discuss it in [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions), or report through our [Telegram Channel](https://t.me/installerx_revived).
    - To lock the installer on Oppo/Vivo, use the lock tool.
    - To install apps through shizuku on Honor devices, disable `Monitor ADB install` in developer settings.

## About Releases

> [!WARNING]
> Development versions may be unstable and features may change/be removed without any notice.
> Switching build channels may require data wipe/reinstallation.

- **`dev` branch:** Contains features under development. If you want to test them, look for the corresponding CI builds in Github Actions.
- **`main` branch:** When stable changes are merged from `dev`, the CI/CD system automatically builds and publishes a new alpha version.
- **Stable releases:** Manually published when finishing a development/testing phase. CI/CD automatically publishes them as a release.
- **About network permission:** As features have expanded, some network-related functions have been introduced. However, many users prefer the installer to remain purely local without requiring network access. Therefore, two versions will be released: **online** and **offline**. Both versions share the same package name, version code, and signature, so they can't be installed side by side (but can be replaced directly). Please download according to your needs.
  - **Online version**: Supports sharing direct download links to InstallerX for installation. More network-related utilities may be added in the future, but network permission will **never** be used for non-installation purposes. Safe to use.
  - **Offline version**: Requests no network permissions at all. When attempting to use online features, you will receive a clear error message. This version remains a purely local installer.

## About Localization

Help us translate this project! You can contribute at: https://hosted.weblate.org/engage/installerx-revived/

### Localization Status

[![Localization Status](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)

## License

Copyright © [iamr0s](https://github.com/iamr0s) and [contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX is currently released under [**GNU General Public License v3 (GPL-3)**](http://www.gnu.org/licenses/gpl-3.0), though this commitment may change in the future. Maintainers reserve the right to modify license terms or the open-source status of the project.

If you base your development on InstallerX, you must comply with the terms of the open-source license of the specific version of the source code you use as a base, regardless of future changes made to the main project.

## Acknowledgements

This project uses code from, or is based on the implementation of, the following projects:

- [iamr0s/InstallerX](https://github.com/iamr0s/InstallerX)
- [tiann/KernelSU](https://github.com/tiann/KernelSU)
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions)
- [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller)
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
