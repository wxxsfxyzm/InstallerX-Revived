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

- **Redesigned UI:** Material 3 Expressive design
- **Bug fixes:** Resolved APK deletion issues on certain systems
- **Multilingual support:** English, Traditional Chinese, and Spanish. Contributions for more languages welcome!
- **Dialog optimization:** Improved installation dialog display
- **New features:**
  - Shows targetSDK and minSDK during installation
  - Displays system icons (method from [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku))
  - Shizuku/Root can bypass launch restrictions on customized systems (dialog installation only)
  - Extended installation dialog menu (enable in settings):
    - Displays requested permissions
    - InstallFlags configuration (based on [InstallWithOptions](https://github.com/zacharee/InstallWithOptions))
      - **Note:** Some options may not work or pose security risks
  - Supports installing APKs from ZIP files (dialog installation only):
    - Semi-intelligent selection of best package to install

## FAQ

- **Dhizuku not working properly**
  - Developers don't personally use Dhizuku and have limited knowledge about it
  - Dhizuku has permission limitations (can't bypass intent blocks or specify installation source)
  - Tested on SDK ≥34 AVDs
  - Recommended to use Shizuku when possible

- **Lock tool not working**
  - Due to package name changes, use the modified [InstallerX Lock Tool](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/main/InstallerX%E9%94%81%E5%AE%9A%E5%99%A8_1.3.apk)

- **HyperOS shows "System app installation requires valid installer declaration" error**
  - System security restriction
  - Declare a system installer (recommended: `com.android.fileexplorer` or `com.android.vending`)
  - Works with Shizuku/Root (Dhizuku not supported)
  - New feature: Auto-detects HyperOS to add installer in default config

- **HyperOS reverts to default installer**
  - HyperOS revokes permissions if user rejects ADB/Shizuku installation
  - Solution: Manually relock the installer

- **Installation notification freezes on HyperOS**
  - HyperOS has strict background app controls
  - Enable "No background restrictions"
  - App automatically closes 5 seconds after completing installation

- **Issues on Oppo/Vivo/Lenovo devices**
  - We don't have these devices for testing. Discuss solutions in [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions)

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
