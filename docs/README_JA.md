# InstallerX Revived (Community Edition)

[English](README.md) | [简体中文](README_CN.md) | [Español](README_ES.md) | **日本語** | [Deutsch](README_DE.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![安定版](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=安定版)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)
[![ベータ版](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=ベータ版)](https://github.com/wxxsfxyzm/InstallerX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

InstallerX Revived は、モダンな Android パッケージインストーラーであり、元の [InstallerX](https://github.com/iamr0s/InstallerX) プロジェクトをコミュニティで継続しているものです。

このローカライズ README は、プロジェクトの変更が速く、古い翻訳済み機能一覧が不正確になる可能性があるため、意図的に簡潔にしています。

最新かつ完全な情報は以下を参照してください:

- [ドキュメント](https://wxxsfxyzm.github.io/InstallerX-Revived-Website/)
- 英語 README: [README.md](README.md)
- 簡体字中国語 README: [README_CN.md](README_CN.md)

## 主な機能

- APK、APKS、APKM、XAPK、ZIP 内 APK、一括 APK インストール。
- ダイアログ、通知、自動、権限がある場合のサイレントインストール。
- 認可方式:
  - **Root:** すべての特権操作を実行できますが、`app_process` のコールドスタートにより遅くなる場合があります。
  - **Shizuku:** 起動方式に応じて shell または root 相当の権限を取得し、通常は直接 Root より高速です。
  - **Dhizuku:** DevicePolicyManager ベースの操作、たとえばデフォルトインストーラーの固定やアプリのインストールを実行できますが、その他の特権操作には制限があります。
  - **None:** 完全にシステムの制限を受けますが、InstallerX がシステムインストーラーとして動作している場合はサイレントインストールできます。
- インストール元ごとのプロファイル、インストールフラグ、対象ユーザー制御、DexOpt、ブラックリスト、署名ポリシー。
- Material 3 Expressive と Miuix の UI、Live Activity、対応 Xiaomi デバイスでの Xiaomi HyperOS 風アイランド通知。

## 対応 Android バージョン

- **完全サポート:** Android SDK 34 - 37.0
- **限定サポート:** Android SDK 26 - 33

## ダウンロード

- **安定版:** https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest
- **Alpha ビルド:** https://github.com/wxxsfxyzm/InstallerX/releases
- **CI ビルド:** https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml
- **Telegram チャンネル:** https://t.me/installerx_revived

不具合を報告する場合は、Stable では既に修正済みの可能性があるため、可能な限り最新の Alpha または CI ビルドで再現してください。

再現可能な不具合や具体的な feature request は [GitHub Issues](https://github.com/wxxsfxyzm/InstallerX-Revived/issues) に投稿してください。良い提案も歓迎します。一般的な質問や互換性に関する相談は [GitHub Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions) または [Telegram チャンネル](https://t.me/installerx_revived) を利用してください。

## ビルド

ローカル debug ビルド:

```bash
./gradlew assembleOnlineUnstableDebug assembleOfflineUnstableDebug
```

別のアプリ ID を使う PR 向けテストビルド:

```bash
./gradlew assembleOnlinePreviewDebug assembleOfflinePreviewDebug -PAPP_ID="com.rosan.installer.x.revived.test"
```

## ローカライズ

[Weblate](https://hosted.weblate.org/engage/installerx-revived/) で InstallerX Revived の翻訳に協力できます。

[![ローカライズ状況](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)
