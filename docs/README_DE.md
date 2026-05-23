# InstallerX Revived (Community Edition)

[English](README.md) | [简体中文](README_CN.md) | [Español](README_ES.md) | [日本語](README_JA.md) | **Deutsch**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)
[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

InstallerX Revived ist ein moderner Android-Paketinstaller und die von der Community gepflegte Fortsetzung des ursprünglichen [InstallerX](https://github.com/iamr0s/InstallerX)-Projekts.

Diese lokalisierte README ist bewusst kurz gehalten, da sich das Projekt schnell weiterentwickelt und ältere übersetzte Funktionslisten ungenau werden können.

Die aktuellsten und vollständigsten Informationen findest du hier:

- Dokumentation: https://wxxsfxyzm.github.io/InstallerX-Revived-Website/
- Englische README: [README.md](README.md)
- README in vereinfachtem Chinesisch: [README_CN.md](README_CN.md)

## Hauptfunktionen

- Installation von APK, APKS, APKM, XAPK, APKs in ZIP-Archiven und Batch-APKs.
- Dialog-, Benachrichtigungs-, automatische und privilegierte Silent-Installationsabläufe.
- Autorisierungsarten:
  - **Root:** kann alle privilegierten Operationen ausführen, kann aber wegen des kalten Starts von `app_process` langsamer sein.
  - **Shizuku:** erhält je nach Aktivierungsart shell- oder rootähnliche Rechte und reagiert meist schneller als direkter Root-Zugriff.
  - **Dhizuku:** kann DevicePolicyManager-basierte Operationen wie das Fixieren des Standardinstallers und App-Installationen ausführen, ist aber bei anderen privilegierten Aufgaben eingeschränkt.
  - **None:** ist vollständig durch das System begrenzt, kann aber lautlos installieren, wenn InstallerX als Systeminstaller läuft.
- Profile pro Installationsquelle, Installationsflags, Zielbenutzersteuerung, DexOpt, Sperrlisten und Signaturrichtlinien.
- Material 3 Expressive und Miuix-Oberflächen, Live Activity und Xiaomi-HyperOS-ähnliche Island-Benachrichtigungen auf unterstützten Xiaomi-Geräten.

## Unterstützte Android-Versionen

- **Vollständige Unterstützung:** Android SDK 34 - 37.0
- **Eingeschränkte Unterstützung:** Android SDK 26 - 33

## Downloads

- **Stabile Versionen:** https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest
- **Alpha-Builds:** https://github.com/wxxsfxyzm/InstallerX/releases
- **CI-Builds:** https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml
- **Telegram-Kanal:** https://t.me/installerx_revived

Wenn du Fehler meldest, reproduziere sie bitte nach Möglichkeit mit dem neuesten Alpha- oder CI-Build, da Probleme aus Stable möglicherweise bereits behoben wurden.

## Build

Lokaler Debug-Build:

```bash
./gradlew assembleOnlineUnstableDebug assembleOfflineUnstableDebug
```

PR-Testbuild mit separater App-ID:

```bash
./gradlew assembleOnlinePreviewDebug assembleOfflinePreviewDebug -PAPP_ID="com.rosan.installer.x.revived.test"
```

## Lokalisierung

Hilf mit, InstallerX Revived auf Weblate zu übersetzen:

https://hosted.weblate.org/engage/installerx-revived/

[![Lokalisierungsstatus](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)
