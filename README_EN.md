# InstallerX Revived (Community Edition)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)

- This is a community-maintained fork after the [original project](https://github.com/iamr0s/InstallerX) was archived by the author
- Provides limited open-source updates and support
- Strictly follows GNU GPLv3 - all modifications are open source

## Introduction

> A modern and functional Android app installer. (You know some birds are not meant to be caged, their feathers are just too bright.)

Looking for a better app installer? Try **InstallerX**!

Many customized Chinese ROMs come with subpar default installers. You can replace them with **InstallerX**.

Compared to stock systems, **InstallerX** offers more installation options:
- Dialog-based installation
- Notification-based installation
- Automatic installation
- Installer declaration
- Option to install for all users
- Allow test packages
- Allow downgrade installation
- Auto-delete APK after installation

## Supported Versions

- SDK 34+ (full support)
- SDK 30-33 (limited support, please report issues)

## Changelog

- Simplified UI
- Fixed APK deletion issues from original repository
- Text adjustments, added Traditional Chinese support. PRs for more languages welcome!
- Improved version number display for apps to be installed
- Added targetSDK and minSDK display during installation

## FAQ

- **HyperOS shows error** `System app installation requires valid installer declaration` **what to do?**
  - System security restriction. You need to declare a system app as installer in settings (recommended: `com.android.fileexplorer` or `com.android.vending`)
  - Planning to add: "Auto-detect HyperOS and add installer to default config"

- **Not working on Oppo/Vivo/Lenovo devices?**
  - We don't have these devices for testing. Please discuss in [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions)

## License

Copyright © [iamr0s](https://github.com/iamr0s) and contributors

InstallerX is currently released under [**GNU GPL v3**](http://www.gnu.org/copyleft/gpl.html), though this licensing approach may change in the future. Maintainers reserve the right to modify license terms or transition to closed-source.

If you create derivative works based on InstallerX, you must comply with the GPL-3.0 terms of the specific version you use as base, regardless of future licensing changes in the main project.