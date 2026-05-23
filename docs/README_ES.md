# InstallerX Revived (Community Edition)

[English](README.md) | [简体中文](README_CN.md) | **Español** | [日本語](README_JA.md) | [Deutsch](README_DE.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Última versión](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Estable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)
[![Beta](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

InstallerX Revived es un instalador moderno de paquetes Android y la continuación mantenida por la comunidad del proyecto original [InstallerX](https://github.com/iamr0s/InstallerX).

Este README localizado se mantiene breve porque el proyecto avanza rápido y las listas de funciones traducidas pueden quedar desactualizadas.

Para obtener la información más reciente y completa, consulta:

- Documentación: https://wxxsfxyzm.github.io/InstallerX-Revived-Website/
- README en inglés: [README.md](README.md)
- README en chino simplificado: [README_CN.md](README_CN.md)

## Capacidades principales

- Instalación de APK, APKS, APKM, XAPK, APK dentro de ZIP e instalación por lotes.
- Flujos de instalación mediante diálogo, notificación, instalación automática e instalación silenciosa con privilegios.
- Autorizadores:
  - **Root:** puede ejecutar todas las operaciones privilegiadas, pero puede ser más lento por el arranque en frío de `app_process`.
  - **Shizuku:** obtiene capacidades shell o root según su modo de activación, y normalmente responde más rápido que Root directo.
  - **Dhizuku:** puede realizar operaciones basadas en DevicePolicyManager, como bloquear el instalador predeterminado e instalar aplicaciones, pero está limitado en otras tareas privilegiadas.
  - **None:** queda completamente limitado por el sistema, pero puede instalar silenciosamente cuando InstallerX funciona como instalador del sistema.
- Perfiles por origen, flags de instalación, control de usuario de destino, DexOpt, listas negras y reglas de política de firma.
- Estilos de interfaz Material 3 Expressive y Miuix, Live Activity y notificaciones estilo isla de Xiaomi HyperOS en dispositivos Xiaomi compatibles.

## Versiones de Android compatibles

- **Soporte completo:** Android SDK 34 - 37.0
- **Soporte limitado:** Android SDK 26 - 33

## Descargas

- **Versiones estables:** https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest
- **Versiones Alpha:** https://github.com/wxxsfxyzm/InstallerX/releases
- **Compilaciones CI:** https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml
- **Canal de Telegram:** https://t.me/installerx_revived

Al reportar errores, intenta reproducirlos con la versión Alpha o CI más reciente siempre que sea posible, porque el problema puede estar ya corregido respecto a Stable.

## Compilación

Para una compilación debug local:

```bash
./gradlew assembleOnlineUnstableDebug assembleOfflineUnstableDebug
```

Para una compilación de prueba estilo PR con un identificador de aplicación separado:

```bash
./gradlew assembleOnlinePreviewDebug assembleOfflinePreviewDebug -PAPP_ID="com.rosan.installer.x.revived.test"
```

## Localización

Ayuda a traducir InstallerX Revived en Weblate:

https://hosted.weblate.org/engage/installerx-revived/

[![Estado de localización](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)
