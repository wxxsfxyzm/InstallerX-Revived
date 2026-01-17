# InstallerX Revived (Community Edition)

[English](README.md) | [简体中文](README_CN.md) | [Español](README_ES.md) | [日本語](README_JA.md) | **Deutsch**

[![License: GPL v3](https://img.shields.io/badge/Lizenz-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Latest Release](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Stable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)
[![Prerelease](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

- Dies ist ein von der Community gepflegter Fork, nachdem das [Originalprojekt](https://github.com/iamr0s/InstallerX) vom Autor archiviert wurde
- Bietet eingeschränkte Open-Source-Updates und Support
- Befolgt strikt die GNU GPLv3 – alle Änderungen sind Open Source
- Community-Beiträge sind herzlich willkommen!

## Einführung

> Ein moderner und funktionaler Android-App-Installer.  
> (Manche Vögel sind nicht dafür gemacht, eingesperrt zu werden – ihre Federn sind einfach zu bunt.)

Du suchst nach einem besseren App-Installer? Probiere **InstallerX**!

Viele stark angepasste chinesische ROMs haben minderwertige Standard-Installer. Diese kannst du durch **InstallerX Revived** ersetzen.

Im Vergleich zu Standard-Installern bietet **InstallerX Revived** mehr funktionen:

- Umfangreiche Installationstypen: APK, APKS, APKM, XAPK, APKs innerhalb von ZIP-Archiven sowie Batch-APKs
- Dialogbasierte Installation
- Benachrichtigungsbasierte Installation (Live Activity API unterstützt)
- Automatische Installation
- Festlegen als Standard-installer
- Setzen von Installations-Flags (kann nach Profileinstellungen standardmäßig festgelegt werden)
- Installation für einen bestimmten Benutzer / alle Benutzer
- Dex2oat nach erfolgreicher Installation
- Blockieren der Installation bestimmter Apps nach packageName oder sharedUID
- Automatisches Löschen der APK nach der Installation
- Keine Shell-Befehle, ausschließlich native API-Aufrufe

## Unterstützte Versionen

- **Vollständige Unterstützung:** Android SDK 34 – 36.1 (Android 14 – 16)
- **Eingeschränkte Unterstützung:** Android SDK 26 – 33 (Android 8.0 – 13) (bitte Probleme melden)

## Wichtige Änderungen und Funktionen

- **UI-Optionen:** Umschaltbar zwischen Material 3 Expressive (Google Design) und Miuix (ähnlich HyperOS)
- **Mehr Anpassungsmöglichkeiten:** Stärker konfigurierbare Oberflächeneinstellungen
- **Fehlerbehebungen:** Behebung von Problemen beim Löschen von APKs aus dem Originalprojekt auf bestimmten Systemen
- **Leistung:** Optimierte Parsing-Geschwindigkeit und verbesserte Verarbeitung verschiedener Pakettypen
- **Mehrsprachige Unterstützung:** Mehr Sprachen werden unterstützt – Beiträge sind willkommen
- **Dialog-Optimierung:** Verbesserte Darstellung der Installationsdialoge
- **System-Icons:** Unterstützung für System-Icon-Pakete während der Installation (umschaltbar)
- **Versionsvergleich:** Anzeige von Versionsnummern im ein- oder mehrzeiligen Format
- **SDK-Informationen:** Anzeige von targetSDK und minSDK im Installationsdialog
- **Session-Installationsbestätigung:** Unterstützung dank [InxLocker](https://github.com/Chimioo/InxLocker) für Store-Apps wie Aurora Store oder F-Droid
- **Umgehung von Beschränkungen:** Shizuku/Root kann OS-Beschränkungen beim App-Start nach der Installation umgehen
  - Aktuell nur bei Dialoginstallation
  - Für Dhizuku wurde ein anpassbarer Countdown hinzugefügt
- **Erweitertes Menü (Dialoginstallation):**
  - Anzeige angeforderter Berechtigungen
  - InstallFlags-Konfiguration (anwendbar aus Profilen)
  - **Hinweis:** InstallFlags funktionieren nicht garantiert und können Sicherheitsrisiken bergen
- **Voreingestellte Quellen:** Vorabkonfiguration von Installationsquellen für schnelle Auswahl
- **Installation aus ZIP:**
  - Unbegrenzte Anzahl an ZIP-Dateien
  - Unterstützung verschachtelter Verzeichnisse
  - Automatische Deduplizierung und intelligente Auswahl
- **Batch-Installation:** Installation mehrerer APKs gleichzeitig (Dialoginstallation)
- **APKS / APKM / XAPK:** Automatische Split-Auswahl
- **Architektur-Unterstützung:** Installation von armeabi-v7a auf arm64-v8a-Systemen (systemabhängig)
- **Downgrade mit/ohne Daten (Android 15):**
  - Nur für Android 15
  - Nutzung über intelligente Vorschläge
  - **Vorsicht bei System-Apps**
- **Blacklist:** Sperrliste für packageName oder sharedUID mit Ausnahmen
- **DexOpt:** Automatisches dex2oat nach Installation (ohne Dhizuku)
- **Signaturprüfung:** Warnung bei nicht übereinstimmenden Signaturen
- **Zielbenutzer:** Installation für bestimmte Benutzer möglich
- **Als Deinstaller deklarieren:** Annahme von Uninstall-Intents (systemabhängig)
- **[Experimentell] Installation über Download-Link:** Online-Version unterstützt direkte APK-Links

## FAQ

> [!NOTE]
> Bitte lies diese FAQ vor dem Melden von Problemen.  
> Gib dabei Gerätehersteller, Systemversion, App-Version und genaue Schritte an.

- **Dhizuku funktioniert nicht richtig?**
    - Die Unterstützung für **offizielles Dhizuku** ist eingeschränkt. Getestet auf AVDs mit SDK ≥34. Der Betrieb auf SDK <34 ist nicht garantiert.
    - Bei Verwendung von `OwnDroid` funktioniert die Funktion `Auto delete after installation` möglicherweise nicht korrekt.
    - Auf chinesischen ROMs treten gelegentliche Fehler meist dadurch auf, dass das System die Hintergrundausführung von Dhizuku einschränkt. Es wird empfohlen, zuerst die Dhizuku-App neu zu starten.
    - Dhizuku verfügt nur über eingeschränkte Berechtigungen. Viele Vorgänge sind nicht möglich (z. B. das Umgehen von System-Intent-Interzeptoren oder das Festlegen der Installationsquelle). Wenn möglich, wird die Nutzung von Shizuku empfohlen.

- **InstallerX lässt sich nicht als Standard-Installer festlegen?**
    - Einige Systeme haben sehr strenge Richtlinien für Paket-Installer. In diesem Fall musst du ein LSPosed-Modul verwenden, um den Intent abzufangen und an den Installer weiterzuleiten.
    - Funktioniert am besten mit: [Chimioo/InxLocker](https://github.com/Chimioo/InxLocker)
    - Andere als LSPosed arbeitende Locker werden nicht mehr empfohlen.

- **Ein Fehler ist in der Auflösungsphase aufgetreten: `No Content Provider` oder `reading provider` meldet `Permission Denial`?**
    - Du hast „Hide app list“ oder ähnliche Funktionen aktiviert. Bitte konfiguriere die Whitelist entsprechend.

- **HyperOS zeigt den Fehler „Installing system apps requires declaring a valid installer“**
    - Dies ist eine Systemsicherheitsbeschränkung. Du musst einen Installer deklarieren, der eine System-App ist (empfohlen: `com.android.fileexplorer` oder `com.android.vending` für HyperOS; App-Store bei Vivo).
    - Funktioniert mit Shizuku/Root. **Dhizuku wird nicht unterstützt**.
    - Neues Feature: InstallerX erkennt HyperOS automatisch und fügt eine Standardkonfiguration (`com.miui.packageinstaller`) hinzu. Du kannst diese bei Bedarf in den Einstellungen ändern.

- **HyperOS installiert den Standard-Installer erneut / Sperren schlägt fehl**
    - Versuche, `Auto Lock Installer` in den Einstellungen zu aktivieren.
    - Auf einigen HyperOS-Versionen ist ein Fehlschlagen der Sperre zu erwarten.
    - HyperOS fängt USB-Installationsanfragen (ADB/Shizuku) mit einem Dialog ab. Wenn der Nutzer die Installation einer neuen App ablehnt, setzt das System die Installer-Einstellung zurück und erzwingt den Standard-Installer. In diesem Fall sperre InstallerX erneut.

- **Der Fortschrittsbalken der Benachrichtigung friert ein**
    - Einige Custom-OS haben sehr strenge Hintergrund-App-Kontrollen. Setze für die App „Keine Hintergrundbeschränkungen“, falls dieses Problem auftritt.
    - Die App ist optimiert: Sie beendet alle Hintergrunddienste und schließt sich 1 Sekunde nach Abschluss der Installationsaufgabe (wenn der Nutzer auf „Done“ klickt oder die Benachrichtigung entfernt). Du kannst die Foreground-Service-Benachrichtigung aktivieren, um den Vorgang zu überwachen.

- **Probleme auf Oppo/Vivo/Lenovo/…-Systemen?**
    - Wir verfügen nicht über Geräte dieser Marken zum Testen. Du kannst dies in den [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions) besprechen oder über unseren [Telegram-Kanal](https://t.me/installerx_revived) melden.
    - Um den Installer auf Oppo/Vivo zu sperren, verwende das Lock-Tool.
    - Um Apps über Shizuku auf Honor-Geräten zu installieren, deaktiviere `Monitor ADB install` in den Entwickleroptionen.

## Über Releases

> [!WARNING]
> Entwicklungsversionen können instabil sein und Funktionen können ohne Vorankündigung geändert oder entfernt werden.

- **`dev`-Branch:** Enthält experimentelle Funktionen
- **`main`-Branch:** Automatische Alpha-Builds
- **Stabile Releases:** Manuell veröffentlicht
- **Online / Offline-Versionen:**
  - **Online:** Unterstützt Download-Links
  - **Offline:** Keine Netzwerkberechtigungen

## Über Lokalisierung

Hilf uns bei der Übersetzung:  
https://hosted.weblate.org/engage/installerx-revived/

### Lokalisierungsstatus

[![Localization Status](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)

## Lizenz

Copyright © [iamr0s](https://github.com/iamr0s) und
[Mitwirkende](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX wird unter der **GNU General Public License v3 (GPL-3)** veröffentlicht.
Die Lizenzbedingungen oder der Open-Source-Status können zukünftig geändert werden.

Bei Weiterentwicklung auf Basis von InstallerX gelten stets die Lizenzbedingungen
der verwendeten Quellcode-Version.

## Danksagungen

Dieses Projekt verwendet Code aus oder basiert auf Implementierungen der folgenden Projekte:

- [iamr0s/InstallerX](https://github.com/iamr0s/InstallerX)
- [tiann/KernelSU](https://github.com/tiann/KernelSU)
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions)
- [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller)
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
