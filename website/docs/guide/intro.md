---
title: What is InstallerX Revived?
description: Learn about InstallerX Revived - a modern, feature-rich Android package installer with universal compatibility and advanced features
---

# What is InstallerX Revived?

InstallerX Revived is a modern, feature-rich package installer for Android. It is a community-maintained continuation of the original InstallerX project, designed to replace the often lackluster default package installers found in customized Android ROMs.

It leverages native Android APIs to provide a seamless installation experience while offering advanced features for power users, such as root-based installation, batch processing, and extensive format support.

## Features

The core philosophy of InstallerX Revived is to provide **Universal Compatibility** combined with **Modern Aesthetics**.

### Universal Installation
Unlike stock installers that often struggle with complex formats, InstallerX Revived handles almost everything. It supports standard **APK** files, split APKs (**APKS**, **APKM**, **XAPK**), and even installing APKs directly from **ZIP** archives without extraction. It also features a robust **Batch Installation** mode, allowing you to install multiple apps at once with automatic deduplication and version control.

### Native & Secure
InstallerX Revived prioritizes stability and security. It avoids using fragile shell commands for standard operations, relying instead on **Native Android APIs**. It includes built-in signature verification to prevent tampering and offers a detailed permission viewer before installation. For users with **Shizuku** or **Root** access, it can perform silent installations and bypass system restrictions (such as interception by custom OS UIs).

### Modern Aesthetics
The app is designed to look and feel like a native part of modern Android. You can switch between a **Material 3 Expressive** design or a **Miuix** (HyperOS-like) interface. It fully supports dark mode, dynamic colors, and can even display system icon packs during the installation dialog or notifications.

### Advanced Power Tools
For enthusiasts, InstallerX Revived offers features not found in standard installers:
* **Per-Source Profiles:** You can customize installation behavior based on the *initiating app*. For example, you can apply different install options (like silent install or specific flags) for a browser versus a file manager.
* **Install Source:** Customize install requester and installer package name
* **Downgrade Support:** On supported devices, it allows app downgrades while preserving user data. (Root is required for Android 15+)
* **Dex2oat Optimization:** Automatically optimizes apps after installation for better performance.
* **Install Flags:** Customize installation flags (e.g., install for specific users, allocate restricted permissions).
* **Package Blacklist:** Prevent specific packages or SharedUIDs from being installed.

## Supported Versions

* **Full Support:** Android 14 - Android 16 (SDK 34 - 36.1)
* **Limited Support:** Android 8.0 - Android 13 (SDK 26 - 33)

## How to use InstallerX Revived?

See [Getting Started](/guide/quick-start).

## Discussion

* **Telegram:** [@installerx_revived](https://t.me/installerx_revived)
* **GitHub Discussions:** [InstallerX Revived Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions)