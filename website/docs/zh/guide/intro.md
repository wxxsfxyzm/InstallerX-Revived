---
title: 什么是 InstallerX Revived?
description: 了解 InstallerX Revived - 现代化的 Android 应用安装器，支持多种包格式和高级功能
---

# 什么是 InstallerX Revived?

InstallerX Revived 是一款现代、功能丰富的 Android 软件包安装器。它是原 InstallerX 项目的社区维护续作，旨在取代定制 Android ROM 中通常功能简陋的默认安装器。

它利用原生 Android API 提供流畅的安装体验，同时为高级用户提供 Root 安装、批量处理和广泛格式支持等高级功能。

## 功能特性

InstallerX Revived 的核心理念是将 **全能兼容** 与 **现代美学** 完美结合。

### 全能安装
不同于经常难以处理复杂格式的原生安装器，InstallerX Revived 几乎支持所有格式。它支持标准 **APK** 文件、拆分 APK (**APKS**, **APKM**, **XAPK**)，甚至无需解压直接安装 **ZIP** 归档中的 APK。它还具有强大的 **批量安装** 模式，允许您一次安装多个应用，并自动执行去重和版本控制。

### 原生与安全
InstallerX Revived 优先考虑稳定性和安全性。它避免在标准操作中使用脆弱的 Shell 命令，而是依赖 **原生 Android API**。它内置签名验证以防止篡改，并在安装前提供详细的权限预览。对于拥有 **Shizuku** 或 **Root** 权限的用户，它可以执行静默安装并绕过系统限制（例如定制 OS UI 的拦截）。

### 现代美学
该应用的设计旨在外观和体验上都完美融入现代 Android 系统。您可以在 **Material 3 Expressive** 设计或 **Miuix** (类 HyperOS) 界面之间切换。它完全支持深色模式、动态取色，甚至可以在安装对话框或通知中显示系统图标包。

### 高级工具
对于发烧友，InstallerX Revived 提供了标准安装器中没有的功能：
* **按来源配置:** 您可以根据 *发起安装的应用* 自定义安装行为。例如，您可以为浏览器和文件管理器分别应用不同的安装选项（如静默安装或特定标记）。
* **安装来源:** 自定义安装请求者 (Install Requester) 和安装器包名 (Installer Package Name)。
* **降级支持:** 在支持的设备上，允许在保留用户数据的同时降级应用。（Android 15+ 需要 Root 权限）。
* **Dex2oat 优化:** 安装后自动优化应用以获得更好的性能。
* **安装标记 (Install Flags):** 自定义安装标记（例如为特定用户安装、分配受限权限）。
* **应用黑名单:** 禁止安装特定的包名或 SharedUID。

## 支持版本

* **完全支持:** Android 14 - Android 16 (SDK 34 - 36.1)
* **有限支持:** Android 8.0 - Android 13 (SDK 26 - 33)

## 如何使用 InstallerX Revived?

请参阅 [快速开始](/zh/guide/quick-start)。

## 讨论与社区

* **Telegram:** [@installerx_revived](https://t.me/installerx_revived)
* **GitHub Discussions:** [InstallerX Revived Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions)