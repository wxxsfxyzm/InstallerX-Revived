# InstallerX Revived (Community Edition)

**English** | [简体中文](README_CN.md) | [Español](README_ES.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

- This is a community-maintained fork after the [original project](https://github.com/iamr0s/InstallerX) was archived by the author
- Provides limited open-source updates and support
- Strictly follows GNU GPLv3 - all modifications are open source
- We welcome community contributions!

## Introduction

> A modern and functional Android app installer. (You know some birds are not meant to be caged, their feathers are just too bright.)

Looking for a better app installer? Try **InstallerX**!

Many customized Chinese ROMs come with subpar default installers. You can replace them with **InstallerX**.

Compared to stock installers, **InstallerX** offers more installation features:
- Rich installation types: APK, APKS, APKM, XAPK, APKs inside ZIP, and batch APKs.
- Dialog-based installation
- Notification-based installation
- Automatic installation
- Installer declaration
- Setting install flags (can inherit Profile settings)
- Install For specific user / all users
- Dex2oat after success installation
- Block installation of specific apps or by sharedUID
- Auto-delete APK after installation
- No shell commands, native api call only

## Supported Versions

- **Full support:** Android SDK 34 - 36 (Android 14 - 16)
- **Limited support:** Android SDK 26 - 33 (Android 8.0 - 13) (please report issues)

## Key Changes and Features

- **UI Options:** Switchable between a new UI design based on Material 3 Expressive and Miuix(Experimental) which is like HyperOS.
- **More Customization:** More customizable interface settings.
- **Bug fixes:** Resolved APK deletion issues from the original project on certain systems.
- **Performance:** Optimized parsing speed, improved parsing of various package types.
- **Multilingual support:** More languages supported. Contributions for more languages are welcome!
- **Dialog optimization:** Improved installation dialog display.
- **System Icons:** Support for displaying system icon packs during installation.
- **Version Comparison:** Support for displaying version number comparison in single-line or multi-line format.
- **SDK Information:** Installation dialogs show targetSDK and minSDK in single-line or multi-line format.
- **Bypass Interceptions:** Shizuku/Root can bypass custom OS chain-start restrictions when opening an App after installation.
    - Currently only works for dialog installation.
    - Dhizuku cannot invoke permissions, so a customizable countdown option was added to reserve time for the app opening action.
- **Extended Menu:** For dialog installation (enable in settings):
    - Displays permissions requested by the application.
    - InstallFlags configuration (can inherit global Profile settings).
      - **Important** Setting InstallFlags **does not guarantee** they will always work. Some options might pose security risks, depending on the system.
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
- **APKS/APKM/XAPK Files:** Support for automatic selection of the best split.
    - Supports both notification and dialog installation.
        - Clicking "Install" in the notification chooses the best option.
        - In the dialog, the best option is selected by default, but can be chosen manually.
    - The split selection interface shows user-friendly descriptions.
- **Architecture Support:** Allows installing armeabi-v7a packages on arm64-v8a only systems. Actual functionality depends on the system providing runtime translation.
- **Downgrade with/without Data:** Support for performing app downgrades with or without data preservation on some OEM Android 15/16 systems.
    - This feature only supports Android 15 and above. On Android 14 or below, try the `Allow downgrade` option in the install options.
    - The feature is available in the smart suggestions of the dialog installation. To use it, first enable the `Show smart suggestions` option.
    - **Use this feature with extreme caution on system apps!** Loss of system app data could let your device unusable.
    - Not compatible with OneUI 7.0, RealmeUI, and some ColorOS versions (AOSP restrictions). If you only see the downgrade option *without* data preservation, it means your system does not support downgrade *with* data.
- **Blacklist:** Support for configuring a list of banned package names for installation in the settings.
    - Support blacklist by packageName / sharedUID with exemptions
    - Blacklist sharedUID 1000/1001 by default, if you don't want this, remove it from the blacklist.
    - `Allow once` in smart suggestions
- **DexOpt:** After successful installation, the app can automatically perform dex2oat on the installed applications according to the configured Profile settings.
    - Not support Dhizuku
- **Signature Verification：** Verify the signature of the installed app and apk to install, and give a warning if they do not match.
- **Select Target User:** Support installing apps to a specific user.
    - Not support Dhizuku
    - Can be overridden by `Install For All Users` install option
- [Testing] **Declare as Uninstaller:** Accept Uninstall intent on certain OS, custom OS may not be supported.
- [Testing] **Directly Install From Download Link:** The online version supports directly sharing the download link of an APK file to InstallerX for installation. Currently, the APK is not kept locally, but an option to retain the installation package will be added in the future.

## FAQ

> [!NOTE]
> Please read the FAQ before providing feedback.
> When providing feedback, please specify your phone brand, system version, software version, and operation in detail.

- **Dhizuku not working properly?**
    - Support for **official Dhizuku** is limited. Tested on AVDs with SDK ≥34. Operation on SDK <34 is not guaranteed.
    - When using `OwnDroid`, the `Auto delete after installation` function might not work correctly.
    - On Chinese ROMs, occasional errors are usually due to the system restricting Dhizuku's background operation. It is recommended to restart the Dhizuku app first.
    - Dhizuku has limited permissions. Many operations are not possible (like bypassing system intent interceptors or specifying the installation source). Using Shizuku is recommended if possible.

- **Lock tool not working?**
    - Due to the package name change, use the modified [InstallerX Lock Tool](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/main/InstallerX%E9%94%81%E5%AE%9A%E5%99%A8_1.3.apk) from this repository.

- An error occurred in the resolving phase: `No Content Provider` or `reading provider` reported `Permission Denial`?
    - You have enabled Hide app list or similar functions, please configure the whitelist.

- **HyperOS shows "Installing system apps requires declaring a valid installer" error**
    - It's a system security restriction. You must declare an installer that is a system app (recommended: `com.android.fileexplorer` or `com.android.vending`).
    - Works with Shizuku/Root. **Dhizuku is not supported**.
    - New feature: InstallerX automatically detects HyperOS and adds a default configuration (`com.miui.packageinstaller`). You can change it in the settings if needed.

- **HyperOS reinstalls the default installer / locking fails**
    - HyperOS may reset the default installer after the user installs an APK-handling app.
    - On some HyperOS versions, locking failure is expected.
    - HyperOS intercepts USB installation requests (ADB/Shizuku) with a dialog. If the user rejects the installation of a new app, the system will revoke the installer setting and force the default one. If this happens, lock InstallerX again.

- **Notification progress bar freezes**
    - Some custom OS has very strict background app controls. Set "No background restrictions" for the app if you encounter this.
    - The app is optimized: it ends all background services and closes 1 seconds after completing the installation task (when the user clicks "Done" or clears the notification). You can enable the foreground service notification to monitor.

- **Problems on Oppo/Vivo/Lenovo/... systems?**
    - We do not have devices from these brands for testing. You can discuss it in [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions), or report through our [Telegram Channel](https://t.me/installerx_revived).
    - To lock the installer on Oppo/Vivo, use the lock tool (Lock Tool).

## About Releases

> [!WARNING]
> Development versions may be unstable and features may change/be removed without any notice.
> Switching build channels may require data wipe/reinstallation.

- **`dev` branch:** Contains features under development. If you want to test them, look for the corresponding builds in [Pull Requests](https://github.com/wxxsfxyzm/InstallerX-Revived/pulls).
  - Changes for each commit are detailed in the PRs (may be AI-generated).
- **`main` branch:** When stable changes are merged from `dev`, the CI/CD system automatically builds and publishes a new alpha version.
- **Stable releases:** Manually published when finishing a development phase and increasing the `versionCode`. CI/CD automatically publishes them as a release.
- **About network permission:** With feature expansion, some network-related functions have been introduced. However, many users prefer the installer to remain purely local without requiring network access. Therefore, two versions will be released: **online** and **offline**. Both versions share the same package name, version code, and signature, and can be installed side by side. Please download according to your needs.
  - **Online version**: Supports sharing direct download links to InstallerX for installation. More network-related utilities may be added in the future, but network permission will **never** be used for non-installation purposes. Safe to use.
  - **Offline version**: Requests no network permissions at all. When attempting to use online features, you will receive a clear error message. This version remains a purely local installer.

## About Localization

Help us translate this project! You can contribute at: https://hosted.weblate.org/projects/installerx-revived/

### Localization Status

| Language            | Status                                                                                                                                                                                                       |
|:--------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **All Languages**   | [![Translation status](https://hosted.weblate.org/widget/installerx-revived/strings/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/)                                         |
| English             | [![Translation status for English](https://hosted.weblate.org/widget/installerx-revived/strings/en/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/en/)                       |
| Simplified Chinese  | [![Translation status for Simplified Chinese](https://hosted.weblate.org/widget/installerx-revived/strings/zh_Hans/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/zh_Hans/)  |
| Traditional Chinese | [![Translation status for Traditional Chinese](https://hosted.weblate.org/widget/installerx-revived/strings/zh_Hant/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/zh_Hant/) |
| Arabic              | [![Translation status for Arabic](https://hosted.weblate.org/widget/installerx-revived/strings/ar/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/ar/)                        |
| French              | [![Translation status for French](https://hosted.weblate.org/widget/installerx-revived/strings/fr/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/fr/)                        |
| German              | [![Translation status for German](https://hosted.weblate.org/widget/installerx-revived/strings/de/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/de/)                        |
| Portuguese (Brazil) | [![Translation status for Portuguese (Brazil)](https://hosted.weblate.org/widget/installerx-revived/strings/pt_BR/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/pt_BR/)     |
| Russian             | [![Translation status for Russian](https://hosted.weblate.org/widget/installerx-revived/strings/ru/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/ru/)                       |
| Spanish             | [![Translation status for Spanish](https://hosted.weblate.org/widget/installerx-revived/strings/es/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/es/)                       |
| Thai                | [![Translation status for Thai](https://hosted.weblate.org/widget/installerx-revived/strings/th/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/th/)                          |
| Ukrainian           | [![Translation status for Ukrainian](https://hosted.weblate.org/widget/installerx-revived/strings/uk/svg-badge.svg)](https://hosted.weblate.org/projects/installerx-revived/strings/uk/)                     |

## License

Copyright © [iamr0s](https://github.com/iamr0s) and [contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX is currently released under [**GNU General Public License v3 (GPL-3)**](http://www.gnu.org/licenses/gpl-3.0), though this commitment may change in the future. Maintainers reserve the right to modify license terms or the open-source status of the project.

If you base your development on InstallerX, you must comply with the terms of the open-source license of the specific version of the source code you use as a base, regardless of future changes made to the main project.

## Acknowledgements

This project uses code from, or is based on the implementation of, the following projects:

- [iamr0s/InstallerX](https://github.com/iamr0s/InstallerX)
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions)
- [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller)
