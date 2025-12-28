# 常见问题 FAQ

::: info 📝 反馈问题须知
在反馈问题之前请先阅读下方常见问题。
反馈时请详细说明：
* **手机品牌与型号**
* **系统版本** (Android 版本 & ROM 版本)
* **软件版本**
* **授权方式** (Shizuku, Dhizuku 或 Root)
* **日志** (如果可能，请使用 logcat 或 LogFox 抓取)
:::

## Dhizuku 无法使用怎么办？
* 请更新到最新版本的 **Dhizuku**。
* 国产 ROM 遇到偶发性报错一般是 Dhizuku 被系统限制了后台，请优先**重启 Dhizuku 应用**后再试。
* Dhizuku 的权限不够大，很多操作无法完成（例如绕过系统 intent 拦截、指定安装来源等）。有条件建议使用 **Shizuku**。

## 没法锁定安装器怎么办？
* 部分系统严格限制第三方安装器，需要使用 **LSPosed 模块**拦截 intent 并转发给 InstallerX。
* 推荐搭配 **[InxLocker](https://github.com/Chimioo/InxLocker)** 使用以获得最佳体验。
* 不再推荐使用其他锁定器模块。

## 分析阶段报错 `No Content Provider` 或 `Permission Denial`？
* 这通常是因为你启用了 **隐藏应用列表 (HMA)** 或类似模块。
* 请配置白名单，允许 InstallerX 访问必要的 Content Provider。

## HyperOS/Vivo 提示 `安装系统app需要申明有效安装者`？
* 这是系统安全限制，需要在配置中声明安装者为系统 App。
    * **HyperOS:** 推荐 `com.android.fileexplorer` 或 `com.android.vending`。
    * **Vivo:** 推荐 Vivo 应用商店的包名。
* **Shizuku/Root** 模式下有效；**Dhizuku** 不支持此功能。
* 本应用在 HyperOS 上启动时会自动添加配置（默认为 `com.miui.packageinstaller`），如果需要更改请在设置中修改。
* 如果开启了 **智能建议**，可以在失败时点击建议选项来绕过此限制继续安装。

## HyperOS 无法锁定安装器 / 锁定失效变回默认？
* 请尝试打开设置中的 **“自动锁定安装器”** 功能。
* 某些 HyperOS 版本受系统限制确实无法锁定，这是正常的。
* HyperOS 会以对话框形式拦截 USB 安装请求 (ADB/Shizuku)。若用户在全新安装一款应用时点击“拒绝”，系统可能会撤销其安装器设定并强行改回默认安装器。若出现这种情况请重新锁定。

## 使用通知安装时，进度条卡住怎么办？
* 一些定制系统对应用后台管控非常严格。如果遇到这种情况，请将 InstallerX 的**后台电量策略**设置为 **“无限制”**。
* 应用已经对后台管理做了优化，在完成安装任务（用户点击完成或清理通知）后延时 **1秒** 自动清理所有后台服务并退出。因此可以放心启用无限制后台，不会造成额外耗电。

## Oppo / Vivo / 联想等系统无法使用？
* 手头没有这些品牌的手机以供测试。遇到问题可以前往 [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions) 或我们的 [Telegram 频道](https://t.me/installerx_revived) 进行讨论。
    * **Oppo/Vivo:** 通常需要配合锁定器模块来替换系统安装器。
    * **荣耀 (Honor):** 使用 Shizuku 安装时，需要关闭开发者选项中的 **“监控 ADB 安装应用”**，否则安装会卡住。

## 如何安装 Magisk / KernelSU 模块？
1. 前往 **设置 -> 实验室**，开启 **“启用模块刷写”** 选项。
2. 使用 InstallerX 打开 ZIP 压缩包（通过文件管理器“打开方式”或“分享”）。

## 如何替换系统包管理器？
* **ColorOS:** 请修改包名为 `com.android.packageinstaller` 并制作成模块刷入。
* **原生 / 类原生 (AOSP):** 除了修改包名，还需要在 `/system/etc/permissions/privapp-permissions-platform.xml` 中补上权限。参考[这个 Issue 评论](https://github.com/wxxsfxyzm/InstallerX-Revived/issues/349#issuecomment-3621922034)。
* **注意:** 作为系统包管理器工作时，**不支持**自定义部分设置（例如指定安装来源）。