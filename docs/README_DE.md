# InstallerX Revived (Community Edition)

[English](README.md) | [简体中文](README_CN.md) | [Español](README_ES.md) | [日本語](README_JA.md) | **Deutsch**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)
[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

InstallerX Revived is a modern Android package installer and the community-maintained continuation of the original [InstallerX](https://github.com/iamr0s/InstallerX) project.

This localized README is intentionally brief because the project is moving quickly and older translated feature lists can become inaccurate.

For the latest and most complete information, please use:

- Official documentation: https://wxxsfxyzm.github.io/InstallerX-Revived-Website/
- English README: [README.md](README.md)
- Simplified Chinese README: [README_CN.md](README_CN.md)

## Key Capabilities

- APK, APKS, APKM, XAPK, ZIP-contained APK, and batch APK installation.
- Dialog, notification, automatic, and privileged silent installation flows.
- Authorizers:
  - **Root:** can perform all privileged operations, but may be slower because of cold `app_process` startup.
  - **Shizuku:** obtains shell or root capabilities depending on how it is activated, and is usually faster than direct Root.
  - **Dhizuku:** can perform DevicePolicyManager-based operations such as default installer locking and app installation, but is limited for other privileged tasks.
  - **None:** is fully limited by the system, but can silently install when InstallerX is running as the system package installer.
- Per-source installation profiles, install flags, target-user control, DexOpt, blacklists, and signature policy gates.
- Material 3 Expressive and Miuix interface styles, Live Activity, and Xiaomi HyperOS-style island notifications on supported Xiaomi devices.

## Supported Android Versions

- **Full support:** Android SDK 34 - 37.0
- **Limited support:** Android SDK 26 - 33

## Downloads

- **Stable releases:** https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest
- **Alpha builds:** https://github.com/wxxsfxyzm/InstallerX/releases
- **CI builds:** https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml
- **Telegram channel:** https://t.me/installerx_revived

When reporting bugs, please reproduce them on the latest Alpha or CI build whenever possible, because issues in Stable may already be fixed.

## Building

For a local debug build:

```bash
./gradlew assembleOnlineUnstableDebug assembleOfflineUnstableDebug
```

For a PR-style test build with a separate application id:

```bash
./gradlew assembleOnlinePreviewDebug assembleOfflinePreviewDebug -PAPP_ID="com.rosan.installer.x.revived.test"
```

## Localization

Help translate InstallerX Revived on Weblate:

https://hosted.weblate.org/engage/installerx-revived/

[![Localization Status](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)
