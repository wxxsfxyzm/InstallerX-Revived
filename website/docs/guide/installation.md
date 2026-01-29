---
title: Installation Guide for InstallerX Revived
description: Step-by-step installation guide for InstallerX Revived Android APK installer
---

# Installation

## Supported Versions

InstallerX Revived is designed for modern Android systems but maintains backward compatibility.

| Android Version | SDK Level | Support Status | Note |
| :--- | :--- | :--- | :--- |
| **Android 14 - 16** | 34 - 36.1 | ✅ **Full Support** | Recommended for best experience. |
| **Android 8.0 - 13** | 26 - 33 | ⚠️ **Limited Support** | Functionality may be limited. Please report issues if encountered. |

---

## Variants (Online vs Offline)

To respect user privacy and diverse needs, we release two variants for every version.
**Note:** Both versions share the same package name, version number, and signature. You can switch between them without uninstalling (dirty flash).

| Variant | Network Permission | Description |
| :--- | :--- | :--- |
| **Online** | ✅ Yes | Supports **online self-updates** (v2.3.1+) and installing APKs via shared download links. <br>Network permission is **strictly** used for installation tasks only. |
| **Offline** | 🚫 No | A pure local installer. It requests **zero** network permissions. <br>Online features will show an error if accessed. |

---

## Release Channels

::: warning ⚠️ Stability Warning
Development versions (Alpha/Dev) do not guarantee stability and features may be added or removed at any time.
**Switching between build channels may require clearing app data or uninstalling.**
:::

### 1. Stable Release
Builds are manually triggered after a development cycle is complete and tested.
* **Best for:** Daily use.
* **Download:** [GitHub Releases](https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest)

### 2. Alpha Release (Main Branch)
Automatically built and released when features are merged into the `main` branch.
* **Best for:** Users who want new features early but need relative stability.
* **Download:**
    * [GitHub Releases (Pre-release)](https://github.com/wxxsfxyzm/InstallerX-Revived/releases)
    * [Telegram Channel](https://t.me/installerx_revived) (Synced automatically)

### 3. Dev Builds (CI/CD)
Automatically built from the `dev` branch. Contains the absolute latest code.
* **Best for:** Testing specific bug fixes or bleeding-edge features.
* **Download:** [GitHub Actions Artifacts](https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml)