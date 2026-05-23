# InstallerX Revived (Community Edition)

**English** | [简体中文](README_CN.md) | [Español](README_ES.md) | [日本語](README_JA.md) | [Deutsch](README_DE.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)
[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

InstallerX Revived is a modern Android package installer and the community-maintained continuation of the original [InstallerX](https://github.com/iamr0s/InstallerX) project.

It is designed to replace limited stock or OEM package installers with a cleaner interface, broader package support, configurable installation profiles, and privileged workflows through Shizuku, Root, Dhizuku, or the system package manager mode.

## Documentation

The full user guide, installation instructions, advanced options, system integration notes, and FAQ are maintained on the official documentation site:

**https://wxxsfxyzm.github.io/InstallerX-Revived-Website/**

## Highlights

- **Package formats:** APK, APKS, APKM, XAPK, APK files inside ZIP archives, and batch APK installation.
- **Install flows:** dialog installation, background notification installation, automatic installation, silent installation when privileges allow it, and Android 16+ Live Activity progress on supported systems.
- **Authorizers:**
  - **Root:** can perform all privileged operations, but may be slower because of cold `app_process` startup.
  - **Shizuku:** obtains shell or root capabilities depending on how it is activated, and is usually faster than direct Root.
  - **Dhizuku:** can perform DevicePolicyManager-based operations such as default installer locking and app installation, but is limited for other privileged tasks.
  - **None:** is fully limited by the system, but can silently install when InstallerX is running as the system package installer.
- **Profiles:** define how installation and uninstallation requests are handled, including install mode, authorizer override, installer/requester metadata, target user, DexOpt, auto-delete behavior, split selection, blacklist policy, and signature gates.
- **System integration:** InstallerX can be locked as the default installer from the Home page status card, used with LSPosed modules such as [InxLocker](https://github.com/Chimioo/InxLocker), or installed as a replacement system package manager by advanced users.
- **Modern UI:** Material 3 Expressive and Miuix interface styles, dark mode, dynamic color, advanced palettes, system icon packs, colorful dialogs, standard notifications, Live Activity, and Xiaomi HyperOS-style island notifications on supported Xiaomi devices.
- **Safety controls:** package-name and SharedUID blacklists, signature mismatch and unknown-signature policy gates, permission preview, install flags, and one-time smart suggestions for selected blocked cases.

## Supported Android Versions

- **Full support:** Android SDK 34 - 37.0
- **Limited support:** Android SDK 26 - 33

Limited support means InstallerX may work, but some features can be unavailable or behave differently because of Android framework, OEM, or authorizer limitations.

## Downloads

- **Stable releases:** https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest
- **Alpha builds:** https://github.com/wxxsfxyzm/InstallerX/releases
- **CI builds:** https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml
- **Telegram channel:** https://t.me/installerx_revived

When reporting bugs, please reproduce them on the latest Alpha or CI build whenever possible, because issues in Stable may already be fixed.

InstallerX is published in two variants:

- **Online:** supports direct APK download links and online update features. Network permission is only used for installation-related features.
- **Offline:** requests no network permission. Online-only features will show a clear error instead.

Both variants share the same package name, version code, and signature, so they replace each other instead of installing side by side.

## Building

InstallerX Revived is an Android Gradle project.

### Prerequisites

- **JDK 25** with `JAVA_HOME` configured correctly.
- Android SDK / Android Studio with the required platform and build tools installed.
- GitHub Packages credentials for the snapshot `miuix` dependency.

### GitHub Packages Authentication

GitHub Packages requires authentication even for public packages. Add your GitHub username and a classic personal access token with the `read:packages` scope to your global Gradle properties file:

- Linux / macOS: `~/.gradle/gradle.properties`
- Windows: `%USERPROFILE%\.gradle\gradle.properties`

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

Do not commit these credentials to this repository.

### Build Commands

For a local debug build:

```bash
./gradlew assembleOnlineUnstableDebug assembleOfflineUnstableDebug
```

For a PR-style test build with a separate application id:

```bash
./gradlew assembleOnlinePreviewDebug assembleOfflinePreviewDebug -PAPP_ID="com.rosan.installer.x.revived.test"
```

## Common Questions

### Where should I report bugs or ask questions?

Use [GitHub Issues](https://github.com/wxxsfxyzm/InstallerX-Revived/issues) for reproducible bugs and concrete feature requests. Good suggestions are welcome. Use [GitHub Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions) or the [Telegram channel](https://t.me/installerx_revived) for general questions and compatibility discussion.

Before opening an issue, please read [CONTRIBUTING.md](../CONTRIBUTING.md) for the required logs and reproduction details.

### I cannot lock InstallerX as the default installer

Some OEM systems strictly control the default package installer. Open the default installer page from the Home page status card and try locking from there. If the ROM still prevents it, use an LSPosed module such as [InxLocker](https://github.com/Chimioo/InxLocker).

### HyperOS says installing system apps requires a valid installer

This is an OEM security restriction. InstallerX can declare installer metadata through profiles, and on HyperOS it uses `com.android.shell` as the default compatibility installer package. Shizuku or Root is required for this workflow; Dhizuku is not enough.

### Notification installation progress is stuck

Some ROMs restrict background services aggressively. Set InstallerX to unrestricted background/battery mode if notification installation stalls. InstallerX cleans up its foreground service shortly after the installation task finishes.

### How do I replace the system package manager?

This is a high-risk advanced workflow. The short version is: use Core Patch to overwrite the APK, flash a matching module, or integrate the matching package into `super` / a ROM build. Before flashing or packaging, verify the package name, mount path, and permissions file for your ROM.

See the system integration guide for details: https://wxxsfxyzm.github.io/InstallerX-Revived-Website/guide/system-integration

## Localization

Help translate InstallerX Revived on Weblate:

https://hosted.weblate.org/engage/installerx-revived/

[![Localization Status](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)

## License

Copyright (C) [iamr0s](https://github.com/iamr0s) and [InstallerX Revived Contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX Revived is released under the [GNU General Public License v3](http://www.gnu.org/licenses/gpl-3.0).

If you base your work on InstallerX Revived, you must comply with the open-source license terms of the specific source version you use.

## Acknowledgements

This project uses code from, or is based on the implementation of, the following projects:

- [iamr0s/InstallerX](https://github.com/iamr0s/InstallerX)
- [tiann/KernelSU](https://github.com/tiann/KernelSU)
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions)
- [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller)
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
