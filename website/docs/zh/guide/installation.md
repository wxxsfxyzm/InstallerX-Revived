# 安装指南

## 支持的版本

InstallerX Revived 专为现代 Android 系统设计，同时也保持了向后兼容性。

| Android 版本 | SDK 等级 | 支持状态 | 备注 |
| :--- | :--- | :--- | :--- |
| **Android 14 - 16** | 34 - 36.1 | ✅ **完全支持** | 推荐使用以获得最佳体验。 |
| **Android 8.0 - 13** | 26 - 33 | ⚠️ **有限支持** | 功能可能受限。如果遇到问题，请提交反馈。 |

---

## 版本说明 (Online vs Offline)

为了尊重用户隐私并满足不同需求，我们为每个版本发布了两个变体。
**注意：** 两个版本拥有相同的包名、版本号和签名。您可以直接覆盖安装进行切换，无需卸载（即 Dirty Flash）。

| 变体 | 联网权限 | 描述 |
| :--- | :--- | :--- |
| **Online (联网版)** | ✅ 有 | 支持 **在线自动更新** (v2.3.1+) 以及通过分享下载链接直接安装 APK。<br>联网权限 **仅严格用于** 安装相关的任务。 |
| **Offline (离线版)** | 🚫 无 | 纯粹的本地安装器。它 **完全不申请** 联网权限。<br>如果尝试使用联网功能将会报错。 |

---

## 发布通道

::: warning ⚠️ 稳定性警告
开发中版本 (Alpha/Dev) 不保证稳定性，功能可能会随时添加或删除。
**在不同构建通道之间切换可能需要清除应用数据或卸载重装。**
:::

### 1. 稳定版 (Stable Release)
在一个开发周期结束并经过测试后手动触发构建。
* **适用人群：** 日常使用。
* **下载地址：** [GitHub Releases](https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest)

### 2. Alpha 测试版 (Main 分支)
当功能合并到 `main` 分支时自动构建并发布。
* **适用人群：** 想要尝鲜新功能但需要相对稳定性的用户。
* **下载地址：**
    * [GitHub Releases (Pre-release)](https://github.com/wxxsfxyzm/InstallerX-Revived/releases)
    * [Telegram 频道](https://t.me/installerx_revived) (自动同步)

### 3. 开发版 (Dev Builds / CI)
由 `dev` 分支自动构建。包含绝对最新的代码。
* **适用人群：** 测试特定的 Bug 修复或最前沿的功能。
* **下载地址：** [GitHub Actions Artifacts](https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml)