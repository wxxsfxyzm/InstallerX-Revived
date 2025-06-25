# InstallerX Revived (Community Edition)

[![README en español](https://img.shields.io/badge/README_ES-0077b5?style=flat-square)](./README_ES.md) [![README in English](https://img.shields.io/badge/README_EN-239120?style=flat-square)](./README_EN.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=稳定版)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=测试版)](https://github.com/wxxsfxyzm/InstallerX/releases)

- 这是一个由社区维护的分支版本， [原项目](https://github.com/iamr0s/InstallerX)已被作者归档。
- 提供有限的开源更新和支持
- 此分支严格遵循 GNU GPLv3，所有修改均开放源代码。

## 介绍

> A modern and functional Android app installer. (You know some birds are not meant to be caged, their feathers are just too bright.) 

一款应用安装程序，为什么不试试【InstallerX】？

在国产系统的魔改下，许多系统的自带安装程序体验并不是很好，你可以使用【InstallerX】替换掉系统默认安装程序。

当然，相对于原生系统，【InstallerX】也带来了更多的安装选项：对话框安装、通知栏安装、自动安装、声明安装者、选择是否安装到所有用户空间、允许测试包、允许降级安装、安装后自动删除安装包。

## 支持版本

SDK 34 以上

SDK 30-33有限支持，有问题发issue

## 变更内容

- UI简化
- 修复了原仓库项目无法正确删除安装包的问题
- 文本调整，支持繁体中文。更多语言欢迎PR
- 修改待安装应用版本号的显示效果
- 加入了安装时显示targetSDK与minSDK的功能

## 常见问题

- HyperOS更新系统应用提示 `安装系统app需要申明有效安装者` 怎么办？
  - 系统安全限制，需要在配置中声明安装者为系统app，推荐 `com.android.fileexplorer` 或 `com.android.vending`
  - 考虑以后增加 `检测到系统为HyperOS自动为Default配置加上安装者` 的功能
 
- Oppo/Vivo/联想的系统用不了了怎么办
  - 手头没有这些品牌的手机，可以前往 [Disscussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions) 进行讨论

## 开源协议

Copyright (C)  [iamr0s](https://github.com/iamr0s) and contributors

InstallerX目前基于 [**GNU General Public License v3 (GPL-3)**](http://www.gnu.org/copyleft/gpl.html)
开源，但不保证未来依然继续遵循此协议或开源，有权更改开源协议或开源状态。

当您选择基于InstallerX进行开发时，需遵循所当前依赖的上游源码所规定的开源协议，不受新上游源码的开源协议影响。