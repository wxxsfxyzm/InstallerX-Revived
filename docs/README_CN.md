# InstallerX Revived (Community Edition)

[English](README.md) | **简体中文** | [Español](README_ES.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=稳定版)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=测试版)](https://github.com/wxxsfxyzm/InstallerX/releases)[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

- 这是一个由社区维护的分支版本， [原项目](https://github.com/iamr0s/InstallerX)已被作者归档。
- 提供有限的开源更新和支持
- 此分支严格遵循 GNU GPLv3，所有修改均开放源代码。
- 我们欢迎社区参与共建！

## 介绍

> A modern and functional Android app installer. (You know some birds are not meant to be caged,
> their feathers are just too bright.)

一款应用安装程序，为什么不试试**InstallerX**？

在国产系统的魔改下，许多系统的自带安装程序体验并不是很好，你可以使用**InstallerX**来安装应用。

当然，相对于原生系统，**InstallerX Revived**也带来了更多功能：

- [x] 丰富的安装类型：apk apks apkm xapk zip包内任意数量的apk，批量分享传入的apk
- [x] 支持解析并安装模块
- [x] 对话框安装、通知栏安装（支持Live Activity API）、自动安装、静默安装
- [x] 声明安装者
- [x] 设定安装选项（可配置，可在安装前修改）
- [x] dex2oat优化
- [x] 为指定用户ID安装/为所有用户安装
- [x] 按照包名/SharedUID禁止安装指定应用
- [x] 安装后自动删除安装包（Dhizuku/None仅支持删除非私有目录文件）
- [x] 不使用shell命令，全部使用原生API调用实现（模块安装相关功能除外）

## 支持版本

支持 Android SDK 34 - 36.1（Android 14 - 16）

对 Android SDK 26 - 33（Android 8.0 - Android 13）提供有限支持，如有问题请提交 issue

## 功能变化

- [x] 可切换基于Material 3 Expressive设计的新UI界面/类HyperOS的Miuix界面
- [x] 更多可自定义化的界面设置
- [x] 修复了原仓库项目在某些系统上无法正确删除安装包的问题
- [x] 优化解析速度，优化各种安装包类型的解析
- [x] 文本调整，支持更多语言。更多语言欢迎参与贡献
- [x] 优化对话框安装的显示效果
- [x] 支持安装时显示系统图标包，支持通过开关在安装包图标/系统图标包之间切换
- [x] 支持单行/多行显示版本号对比
- [x] 安装对话框支持显示targetSDK与minSDK，点击可切换单行/多行
- [x] 支持接管系统安装器确认会话安装，需要和[InxLocker](https://github.com/Chimioo/InxLocker)配合使用
- [x] Shizuku/Root安装完成打开App时可以绕过定制UI的链式启动拦截
    - 目前仅实现了对话框安装
    - Dhizuku无法调用权限，因此加了一个倒计时自定义选项，给打开app的操作预留一定时间
- [x] 为对话框安装提供一个扩展菜单，可以在设置中启用
    - 支持查看应用申明的权限
    - 支持设定InstallFlags（可以继承全局Profile设置）
        - **注意**：设定InstallFlags并不能保证一定生效，部分选项有可能带来安全风险，具体取决于系统
- [x] 支持在设置中预设安装来源的包名，并可以在配置文件和对话框安装菜单中快速选择
- [x] 支持安装zip压缩包内的apk文件，用 InstallerX 打开zip压缩包即可
    - 仅支持对话框安装
    - 不限制数量，支持同时传入多个zip，支持zip内嵌套目录中的apk文件，**不仅限于根目录**
    - 支持自动处理相同包名的多版本
        - 支持去重
        - 支持智能地选择最佳安装包
- [x] 支持批量安装（多选然后共享到InstallerX）
    - 仅支持对话框安装
    - 不限制数量
    - 仅支持apk文件
    - 支持自动处理相同包名的多版本
        - 支持去重
        - 支持智能地选择最佳安装包
- [x] APKS/APKM/XAPK文件支持自动选择最佳分包
    - 同时支持状态栏通知安装&对话框安装
        - 通知栏点击安装即是最优选择
        - 对话框默认选中最优选择，仍可以通过菜单自由选择分包
    - 分包选择界面支持用户友好描述
- [x] 支持在arm64-v8a/X86_64 only的系统中安装armeabi-v7a,armeabi/X86架构的安装包（实际能否运行取决于系统是否提供运行时转译器）
- [x] 支持在部分oem（开启系统优化的HyperOS）的Android15/16系统上保留数据降级安装/不保留数据降级安装
    - 该功能仅支持Android15，Android14请尝试安装选项中的`允许降级安装`。
    - 该功能在对话框安装的智能建议中，需要体验请先打开`显示智能建议`选项
    - 该功能禁止/请谨慎用于系统app，误操作导致系统应用数据丢失可能会导致系统无法正常使用
    - 不适用于OneUI7.0、RealmeUI、部分ColorOS（AOSP已经修复），已经针对性屏蔽。如果只看见不保留数据降级安装选项，说明你的系统不支持保留数据降级安装
- [x] 支持在设置中设定禁止安装的包名列表，设定在列表中的应用将被拒绝安装
    - 支持禁止安装指定包名app
    - 支持按照sharedUid禁止安装，支持在此模式下设定排除项，也允许通过智能建议临时绕过一次（这在HyperOS阻止错误地安装不同机型的系统软件时格外有用）
- [x] 在安装完后可以自动根据配置设定对安装应用进行dex2oat
    - 不支持Dhizuku
- [x] 支持在安装前验证签名，若不匹配会给出警告
- [x] 支持为指定用户安装应用
    - 不支持Dhizuku
    - 可以被 `为所有用户安装` 安装选项覆盖
- [x] 申明自身为卸载工具，可以接受并执行系统卸载请求（绝大多数系统写死卸载器，需要搭配锁定器模块一起使用）
- [x] [实验性] 联网版本支持直接分享安装包文件的下载直链到InstallerX进行安装，目前使用单线程下载，安装包不会保留在本地，以后会加入保留安装包选项

## 常见问题 FAQ

> [!NOTE]
> 在反馈问题之前请先阅读常见问题。
> 反馈时请详细说明自己的手机品牌，系统版本，使用的软件版本，授权器种类以及操作。
> 如果可能请附上日志（使用 logcat/logfox 抓取）

### Dhizuku无法使用怎么办

- 请更新到最新版本的 Dhizuku
- 国产ROM遇到偶发性报错一般是 Dhizuku 被系统限制了后台，请优先重启 Dhizuku 应用后再试
- Dhizuku 的权限不够大，很多操作无法完成，例如绕过系统 intent 拦截，指定安装来源等，有条件建议使用 Shizuku

### 没法锁定安装器怎么办

- 部分系统严格限制安装器，需要使用LSP模块拦截intent并转发给安装器
- 和[Chimioo/InxLocker](https://github.com/Chimioo/InxLocker)一起使用时效果最佳
- 不再推荐使用其他锁定器模块

### 分析阶段报错`No Content Provider`或`reading provider`报错`Permission Denial`

- 你启用了`隐藏应用列表`或类似功能，请配置白名单

### HyperOS/Vivo更新系统应用提示 `安装系统app需要申明有效安装者` 怎么办？

- 系统安全限制，需要在配置中声明安装者为系统app，HyperOS推荐 `com.android.fileexplorer` 或 `com.android.vending`，Vivo推荐应用商店
- Shizuku/Root有效，Dhizuku不支持
- 本应用在HyperOS上启动时会自动添加配置，默认为`com.miui.packageinstaller`，如果需要更改请在设置中修改
- 如果打开智能建议可以在失败时点击建议选项继续安装

### HyperOS无法锁定安装器/锁定失效变回系统默认安装器怎么办

- 请尝试打开设置中的“自动锁定安装器”功能
- 某些 HyperOS 版本无法锁定是正常的
- HyperOS 会以对话框形式拦截USB安装请求(adb/shizuku)，若用户在全新安装一款应用时点击拒绝安装，系统可能会撤销其安装器设定并强行改回默认安装器，若出现这种情况请重新锁定

### 使用通知安装的时候，通知进度条卡住怎么办

- 一些定制系统对应用后台管控非常严格，如果遇到这种情况请设置后台无限制
- 应用已经对后台管理做了优化，在完成安装任务（用户点击完成或清理通知）后延时1秒自动清理所有后台服务并退出，因此可以放心启用无限制后台，不会造成额外耗电，前台服务通知可以保留，以便观察服务运行状态

### Oppo/Vivo/联想/...的系统用不了了怎么办

- 手头没有这些品牌的手机以供测试，遇到问题可以前往 [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions)
  或[Telegram 频道](https://t.me/installerx_revived)进行讨论
    - Oppo，Vivo锁定安装器请使用锁定器
    - 荣耀机型使用 Shizuku 安装需要关闭开发者选项中的 `监控ADB安装应用`，否则安装会卡住

### 为什么不能安装模块/如何使用安装模块功能

- 需要去实验室中开启选项`启用模块刷写`，
- 需要使用 `Installerx-Revived` 打开压缩包或是通过分享来打开压缩包

### 如何替换系统包管理器？

- ColorOS请修改包名为 `com.android.packageinstaller` 并做成模块刷入
- 原生/类原生系统除了要修改包名，还需要参考[这个issue](https://github.com/wxxsfxyzm/InstallerX-Revived/issues/349#issuecomment-3621922034)
  ，在 `/system/etc/permissions/privapp-permissions-platform.xml` 中补上权限，
- 作为系统包管理器工作时，**不支持**自定义部分设置，例如安装来源

## 关于版本发布

> [!WARNING]
> 开发中的版本不对稳定性提供保障，可能会随时添加/删除功能。
> 当切换构建频道的时候，可能会需要清除数据/卸载重新安装。

- 开发中的功能将提交到`dev`分支，如有测试意愿可以前往 Github Actions 寻找相关的CI构建
- 开发完成的功能会合并到`main`分支，CI/CD会自动构建并发布为最新alpha版本
- 稳定版会在一个阶段的开发/测试结束时手动触发构建并由CI/CD自动发布为release
- 关于联网权限：由于功能扩展，引入了联网相关功能，然而许多用户希望安装器保持纯粹的本地安装，不需要联网权限。因此发布时会打包成online和offline两个版本，两个版本的包名、版本号、签名完全相同，可以混装，请按需下载。
    - `online版` 支持分享下载直链到InstallerX进行安装，v2.3.1 版本起支持在线更新，**永远**不会将联网权限用于非安装用途，请放心使用
    - `offline版` 完全不申请联网权限，尝试online版功能时会得到明确的出错提示，做一个纯粹的本地安装器

## 关于本地化

欢迎参与贡献翻译，请前往：https://hosted.weblate.org/engage/installerx-revived/

### 本地化状态

[![翻译状态](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)

## 开源协议

Copyright (C)  [iamr0s](https://github.com/iamr0s)
and [Contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX目前基于 [**GNU General Public License v3 (GPL-3)**](http://www.gnu.org/copyleft/gpl.html)
开源，但不保证未来依然继续遵循此协议或开源，有权更改开源协议或开源状态。

当您选择基于InstallerX进行开发时，需遵循所当前依赖的上游源码所规定的开源协议，不受新上游源码的开源协议影响。

## 致谢

本项目使用了以下项目的代码或参考其实现：

- [iamr0s/InstallerX](https://github.com/iamr0s/InstallerX)
- [tiann/KernelSU](https://github.com/tiann/KernelSU)
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions)
- [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller)
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
