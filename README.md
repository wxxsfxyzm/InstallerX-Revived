# InstallerX Revived (Community Edition)

[![README en español](https://img.shields.io/badge/README_ES-0077b5?style=flat-square)](./README_ES.md) [![README in English](https://img.shields.io/badge/README_EN-239120?style=flat-square)](./README_EN.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=稳定版)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=测试版)](https://github.com/wxxsfxyzm/InstallerX/releases)[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

- 这是一个由社区维护的分支版本， [原项目](https://github.com/iamr0s/InstallerX)已被作者归档。
- 提供有限的开源更新和支持
- 此分支严格遵循 GNU GPLv3，所有修改均开放源代码。

## 介绍

> A modern and functional Android app installer. (You know some birds are not meant to be caged,
> their feathers are just too bright.)

一款应用安装程序，为什么不试试【InstallerX】？

在国产系统的魔改下，许多系统的自带安装程序体验并不是很好，你可以使用【InstallerX】替换掉系统默认安装程序。

当然，相对于原生系统，【InstallerX】也带来了更多的安装选项：对话框安装、通知栏安装、自动安装、声明安装者、选择是否安装到所有用户空间、允许测试包、允许降级安装、安装后自动删除安装包。

## 支持版本

支持 Android SDK 34 - 36（Android 14 - 16）

对 Android SDK 21 - 33（Android 5.0 - Android 13）提供有限支持，如有问题请提交 issue

## 功能变化

- [测试中]可切换经典界面/基于Material 3 Expressive设计的新UI界面
- 更多可自定义化的界面设置
- 修复了原仓库项目在某些系统上无法正确删除安装包的问题
- 优化解析速度，优化各种安装包类型的解析
- 文本调整，支持英文，繁体中文，西班牙语。更多语言欢迎提交PR
- 优化对话框安装的显示效果
- 支持安装时显示系统图标包，方法来自[RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku/blob/master/manager/src/main/java/moe/shizuku/manager/utils/AppIconCache.kt)
- 支持单行/多行显示版本号对比
- 安装对话框支持显示targetSDK与minSDK
- Shizuku/Root安装完成打开App时可以绕过定制UI的链式启动拦截
    - 使用原生api实现，不使用shell命令
    - 目前仅实现了对话框安装
    - Dhizuku无法调用权限，因此加了一个倒计时自定义选项，给打开app的操作预留一定时间
- 为对话框安装提供一个扩展菜单，可以在设置中启用
    - 支持查看应用申明的权限
    - 支持设定InstallFlags（可以继承全局Profile设置）部分实现来自[zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions/blob/main/app/src/main/java/dev/zwander/installwithoptions/data/InstallOption.kt)
       - **注意**：设定InstallFlags并不能保证一定生效，部分选项有可能带来安全风险，具体取决于系统
- 支持在设置中预设安装来源的包名，并可以在配置文件和对话框安装菜单中快速选择
- 支持安装zip压缩包内的apk文件，用 InstallerX 打开zip压缩包即可 
    - 仅支持对话框安装
    - 不限制数量，支持zip内嵌套目录中的apk文件，**不仅限于根目录**
    - 支持自动处理相同包名的多版本
       - 支持去重
       - 支持智能地选择最佳安装包
- 支持批量安装（多选然后共享到InstallerX）
    - 仅支持对话框安装
    - 不限制数量
    - 仅支持apk文件
    - 支持自动处理相同包名的多版本
       - 支持去重
       - 支持智能地选择最佳安装包
- APKS/APKM/XAPK文件支持自动选择最佳分包 部分思路和代码来自[vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller/tree/master/app)
    - 同时支持状态栏通知安装&对话框安装
        - 通知栏点击安装即是最优选择
        - 对话框默认选中最优选择，仍可以通过菜单自由选择分包
    - 分包选择界面支持用户友好描述 
- [测试中] 支持在arm64-v8a only的系统中安装armeabi-v7a的安装包（实际能否运行取决于系统是否提供运行时转译器）
- [测试中] 支持在部分oem的Android15系统上保留数据降级安装/不保留数据降级安装
    - 该功能仅支持Android14以上，Android14请优先尝试安装选项中的`允许降级安装`，失败后再点击建议尝试该功能
    - 该功能在对话框安装的智能建议中，需要体验请先打开`显示智能建议（实验性）`选项
    - 该功能禁止/请谨慎用于系统app，误操作导致系统应用数据丢失可能会导致系统无法正常使用
    - 不适用于OneUI7.0、RealmeUI、部分ColorOS（oem限制），已经针对性屏蔽。如果只看见不保留数据降级安装选项，说明你的系统不支持保留数据降级安装
- [测试中] 支持在设置中设定禁止安装的包名列表，设定在列表中的应用将被拒绝安装
    - 开发中，目前只能手动添加，以后会根据机型出一个默认阻止安装的列表（这在HyperOS阻止错误地安装不同机型的系统软件时格外有用） 

## 常见问题

- Dhizuku无法使用怎么办
    - 目前仅对**官方Dhizuku**提供最低限度的支持，在SDK34以上AVD均有测试，SDK34以下无法保证
    - 使用`OwnDroid`时可能无法正确调用`安装完成后自动删除`功能
    - 国产ROM遇到偶发性报错一般是Dhizuku被系统限制了后台，请优先重启Dhizuku应用后再试
    - Dhizuku的权限不够大，很多操作无法完成，例如绕过系统intent拦截，指定安装来源等，有条件建议使用Shizuku

- 锁定器无法锁定怎么办
    - 由于包名改变，需要使用本仓库的修改版锁定器[InstallerX Lock Tool](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/main/InstallerX%E9%94%81%E5%AE%9A%E5%99%A8_1.3.apk)

- 分析阶段报错`No Content Provider`
    - 你启用了`隐藏应用列表`或类似功能，请配置白名单

- HyperOS更新系统应用提示 `安装系统app需要申明有效安装者` 怎么办？
    - 系统安全限制，需要在配置中声明安装者为系统app，推荐 `com.android.fileexplorer` 或 `com.android.vending`
    - Shizuku/Root有效，Dhizuku不支持
    - 本应用在HyperOS上启动时会自动添加配置，默认为`com.miui.packageinstaller`，如果需要更改请在设置中修改

- HyperOS无法锁定安装器/锁定失效变回系统默认安装器怎么办
    - HyperOS在某些时候会撤销用户对安装器的设定，原因尚待调查
    - 某些HyperOS版本无法锁定是正常的
    - HyperOS会以对话框形式拦截USB安装请求(adb/shizuku)，若用户在全新安装一款应用时点击拒绝安装，系统会撤销其安装器设定并强行改回默认安装器，若出现这种情况请重新锁定
    
- HyperOS使用通知安装的时候，通知进度条卡住怎么办
    - HyperOS对应用后台管控非常严格，如果遇到这种情况请设置后台无限制
    - 应用已经对后台管理做了优化，在完成安装任务（用户点击完成或清理通知）后延时0.5秒自动清理所有后台服务并退出，因此可以放心启用无限制后台，不会造成额外耗电，前台服务通知可以保留，以便观察服务运行状态

- Oppo/Vivo/联想/...的系统用不了了怎么办
    - 手头没有这些品牌的手机，可以前往 [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions)进行讨论
    - Oppo，Vivo锁定安装器请使用锁定器

## 关于版本发布

> [!WARNING]
> 开发中的版本不对稳定性提供保障，可能会随时添加/删除功能。
> 当切换构建频道的时候，可能会需要清除数据/卸载重新安装。

- 开发中的功能将提交到`dev`分支，如有测试意愿可以前往[Pull Request](https://github.com/wxxsfxyzm/InstallerX-Revived/pulls)寻找相关的CI构建
  - 每次commit的变更内容会在PR中提供，可能使用AI生成
- 开发完成的功能会合并到`main`分支，CI/CD会自动构建并发布为最新alpha版本
- 稳定版会在一个阶段的开发结束，需要提高`versionCode`时手动触发构建并由CI/CD自动发布为release

## 开源协议

Copyright (C)  [iamr0s](https://github.com/iamr0s) and [Contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX目前基于 [**GNU General Public License v3 (GPL-3)**](http://www.gnu.org/copyleft/gpl.html)
开源，但不保证未来依然继续遵循此协议或开源，有权更改开源协议或开源状态。

当您选择基于InstallerX进行开发时，需遵循所当前依赖的上游源码所规定的开源协议，不受新上游源码的开源协议影响。
