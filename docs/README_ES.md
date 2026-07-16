# InstallerX Revived (Community Edition)

[English](README.md) | [简体中文](README_CN.md) | **Español** | [日本語](README_JA.md) | [Deutsch](README_DE.md)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Última versión](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Estable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)
[![Beta](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)
[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

> Un instalador de aplicaciones Android moderno y funcional. (Sabes que algunos pájaros no están hechos para estar enjaulados, sus plumas son demasiado brillantes.)

¿Buscas un mejor instalador de aplicaciones? Prueba **InstallerX**.

InstallerX Revived es un instalador moderno de paquetes Android y la continuación mantenida por la comunidad del proyecto original [InstallerX](https://github.com/iamr0s/InstallerX).

Está diseñado para reemplazar instaladores stock u OEM limitados con una interfaz más clara, soporte de paquetes más amplio, perfiles de instalación configurables y flujos privilegiados mediante Shizuku, Root, Dhizuku o el modo de instalador del sistema.

## Documentación

La guía de usuario completa, instrucciones de instalación, opciones avanzadas, notas de integración del sistema y FAQ se mantienen en el [sitio de documentación](https://wxxsfxyzm.github.io/InstallerX-Revived-Website/).

## Capacidades principales

- **Formatos de paquete:** APK, APKS, APKM, XAPK, APK dentro de ZIP e instalación por lotes.
- **Flujos de instalación:** instalación por diálogo, instalación en segundo plano con notificación, instalación automática, instalación silenciosa cuando los privilegios lo permiten y progreso Live Activity en Android 16+ en sistemas compatibles.
- **Autorizadores:**
  - **Root:** puede ejecutar todas las operaciones privilegiadas, pero puede ser más lento por el arranque en frío de `app_process`.
  - **Shizuku:** obtiene capacidades shell o root según su modo de activación, y normalmente responde más rápido que Root directo.
  - **Dhizuku:** puede realizar operaciones basadas en DevicePolicyManager, como bloquear el instalador predeterminado e instalar aplicaciones, pero está limitado en otras tareas privilegiadas.
  - **None:** queda completamente limitado por el sistema, pero puede instalar silenciosamente cuando InstallerX funciona como instalador del sistema.
- **Perfiles:** definen cómo se procesan las solicitudes de instalación y desinstalación, incluido el modo de instalación, override del autorizador, metadatos de instalador/solicitante, usuario de destino, DexOpt, borrado automático, selección de splits, listas negras y reglas de firma.
- **Integración del sistema:** InstallerX puede fijarse como instalador predeterminado desde la tarjeta de estado de la pantalla principal, usarse con módulos LSPosed como [InxLocker](https://github.com/Chimioo/InxLocker), o instalarse como reemplazo del instalador del sistema para usuarios avanzados.
- **Interfaz moderna:** Material 3 Expressive y Miuix, modo oscuro, color dinámico, paletas avanzadas, paquetes de iconos del sistema, diálogos con color, notificaciones estándar, Live Activity y notificaciones estilo isla de Xiaomi HyperOS en dispositivos Xiaomi compatibles.
- **Controles de seguridad:** listas negras por nombre de paquete y SharedUID, reglas para firma no coincidente o desconocida, vista previa de permisos, flags de instalación y sugerencias inteligentes de un solo uso para algunos bloqueos.

## Versiones de Android compatibles

- **Soporte completo:** Android SDK 34 - 37.0
- **Soporte limitado:** Android SDK 26 - 33

Soporte limitado significa que InstallerX puede funcionar, pero algunas funciones pueden no estar disponibles o comportarse de forma distinta por limitaciones de Android, OEM o el autorizador.

## Descargas

- **Versiones estables:** https://github.com/wxxsfxyzm/InstallerX-Revived/releases/latest
- **Versiones Alpha:** https://github.com/wxxsfxyzm/InstallerX/releases
- **Compilaciones CI:** https://github.com/wxxsfxyzm/InstallerX-Revived/actions/workflows/auto-preview-dev.yml
- **Canal de Telegram:** https://t.me/installerx_revived

Al reportar errores, intenta reproducirlos con la versión Alpha o CI más reciente siempre que sea posible, porque el problema puede estar ya corregido respecto a Stable.

InstallerX se publica en dos variantes:

- **Online:** admite enlaces directos de descarga de APK y funciones de actualización en línea. El permiso de red solo se usa para funciones relacionadas con la instalación.
- **Offline:** no solicita permiso de red. Las funciones exclusivas de Online mostrarán un error claro.

Ambas variantes comparten el mismo nombre de paquete, código de versión y firma, por lo que se reemplazan entre sí y no se instalan en paralelo.

## Compilación

InstallerX Revived es un proyecto Android Gradle.

### Requisitos previos

- **JDK 25** con `JAVA_HOME` configurado correctamente.
- Android SDK / Android Studio con la plataforma y herramientas de compilación necesarias.
- Credenciales de GitHub Packages para la dependencia snapshot `miuix`.

### Autenticación de GitHub Packages

GitHub Packages requiere autenticación incluso para paquetes públicos. Añade tu usuario de GitHub y un token personal clásico con el scope `read:packages` al archivo global de propiedades de Gradle:

- Linux / macOS: `~/.gradle/gradle.properties`
- Windows: `%USERPROFILE%\.gradle\gradle.properties`

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

No confirmes estas credenciales en este repositorio.

### Comandos de compilación

Para una compilación debug local:

```bash
./gradlew assembleOnlineUnstableDebug assembleOfflineUnstableDebug
```

Para una compilación de prueba estilo PR con un identificador de aplicación separado:

```bash
./gradlew assembleOnlinePreviewDebug assembleOfflinePreviewDebug -PAPP_ID="com.rosan.installer.x.revived.test"
```

## Preguntas comunes

### ¿Dónde debo reportar errores o hacer preguntas?

Usa [GitHub Issues](https://github.com/wxxsfxyzm/InstallerX-Revived/issues) para bugs reproducibles y feature requests concretas. Las buenas sugerencias también son bienvenidas. Usa [GitHub Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions) o el [canal de Telegram](https://t.me/installerx_revived) para preguntas generales y discusiones de compatibilidad.

Antes de abrir un issue, lee [CONTRIBUTING.md](../CONTRIBUTING.md) para conocer los logs y detalles de reproducción requeridos.

### No puedo fijar InstallerX como instalador predeterminado

Algunos sistemas OEM controlan estrictamente el instalador predeterminado. Abre la página del instalador predeterminado desde la tarjeta de estado de la pantalla principal e intenta fijarlo allí. Si la ROM todavía lo impide, usa un módulo LSPosed como [InxLocker](https://github.com/Chimioo/InxLocker).

### HyperOS dice que instalar aplicaciones del sistema requiere un instalador válido

Es una restricción de seguridad del OEM. InstallerX puede declarar metadatos de instalador mediante perfiles, y en HyperOS usa `com.android.shell` como paquete instalador de compatibilidad predeterminado. Este flujo requiere Shizuku o Root; Dhizuku no es suficiente.

### El progreso de instalación por notificación se queda atascado

Algunas ROM restringen agresivamente los servicios en segundo plano. Configura InstallerX sin restricciones de batería/segundo plano si la instalación por notificación se atasca. InstallerX limpia su servicio en primer plano poco después de terminar la instalación.

### ¿Cómo reemplazo el instalador del sistema?

Es un flujo avanzado de alto riesgo. En resumen: usa Core Patch para sobrescribir el APK, flashea un módulo compatible o integra el paquete correspondiente en `super` / una compilación de ROM. Antes de flashear o empaquetar, verifica el nombre de paquete, la ruta de montaje y el archivo de permisos de tu ROM.

Consulta la guía de integración del sistema: https://wxxsfxyzm.github.io/InstallerX-Revived-Website/guide/system-integration

## Localización

Ayuda a traducir InstallerX Revived en [Weblate](https://hosted.weblate.org/engage/installerx-revived/).

[![Estado de localización](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)

## Licencia

Copyright (C) [iamr0s](https://github.com/iamr0s) and [InstallerX Revived Contributors](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX Revived se publica bajo la [GNU General Public License v3](http://www.gnu.org/licenses/gpl-3.0).

Si basas tu trabajo en InstallerX Revived, debes cumplir los términos de la licencia de código abierto de la versión específica del código fuente que uses.

## Agradecimientos

Este proyecto usa código de, o se basa en la implementación de, los siguientes proyectos:

- [iamr0s/InstallerX](https://github.com/iamr0s/InstallerX)
- [tiann/KernelSU](https://github.com/tiann/KernelSU)
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions)
- [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller)
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)

## Star History

<a href="https://www.star-history.com/?repos=wxxsfxyzm%2FInstallerX-Revived&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=wxxsfxyzm/InstallerX-Revived&type=date&theme=dark&legend=top-left&sealed_token=Hp_xa69sA4KNmnl8KwAi2TAJjRZIIXpI5DPfwe6ULCpe2Pd8lyr0waU2bPCAvZLLDbLc1BlQRnH1U41HhtaLsI7tyxICKwqGErPPLjnj4nJT-SJhIOi6lP5mfsymarUIh2ZcACpqUBL5p32Xd6RHLsZXvOVz3OCwdVMjrQ8XSyOKAAq-0nxFt7qJlGuU" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=wxxsfxyzm/InstallerX-Revived&type=date&legend=top-left&sealed_token=Hp_xa69sA4KNmnl8KwAi2TAJjRZIIXpI5DPfwe6ULCpe2Pd8lyr0waU2bPCAvZLLDbLc1BlQRnH1U41HhtaLsI7tyxICKwqGErPPLjnj4nJT-SJhIOi6lP5mfsymarUIh2ZcACpqUBL5p32Xd6RHLsZXvOVz3OCwdVMjrQ8XSyOKAAq-0nxFt7qJlGuU" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=wxxsfxyzm/InstallerX-Revived&type=date&legend=top-left&sealed_token=Hp_xa69sA4KNmnl8KwAi2TAJjRZIIXpI5DPfwe6ULCpe2Pd8lyr0waU2bPCAvZLLDbLc1BlQRnH1U41HhtaLsI7tyxICKwqGErPPLjnj4nJT-SJhIOi6lP5mfsymarUIh2ZcACpqUBL5p32Xd6RHLsZXvOVz3OCwdVMjrQ8XSyOKAAq-0nxFt7qJlGuU" />
 </picture>
</a>
