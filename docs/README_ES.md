# InstallerX Revived (Edición Comunitaria)

[English](README.md) | [简体中文](README_CN.md) | **Español** | [日本語](README_JA.md) | [Deutsch](README_DE.md)

[![Licencia: GPL v3](https://img.shields.io/badge/Licencia-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Última versión](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Estable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Beta](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Beta)](https://github.com/wxxsfxyzm/InstallerX/releases)[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

- Esto es un fork mantenido por la comunidad después de que el [proyecto original](https://github.com/iamr0s/InstallerX) fuera archivado por el autor.
- Proporciona actualizaciones y soporte limitados de código abierto.
- Sigue estrictamente la GNU GPLv3: todas las modificaciones son de código abierto.
- ¡Damos la bienvenida a las contribuciones de la comunidad!

## Introducción

> Un instalador de aplicaciones Android moderno y funcional. (Sabes que algunos pájaros no están hechos para estar enjaulados, sus plumas son demasiado brillantes.)

¿Buscas un mejor instalador de aplicaciones? ¡Prueba **InstallerX Revived**!

Muchas ROMs chinas personalizadas vienen con instaladores predeterminados deficientes. Puedes reemplazarlos con **InstallerX Revived**.

En comparación con los instaladores de stock, **InstallerX Revived** ofrece más funciones de instalación:
- Tipos de instalación ricos: APK, APKS, APKM, XAPK, APKs dentro de ZIP y APKs en lote.
- Instalación basada en diálogo.
- Instalación basada en notificación (compatible con API de Live Activity).
- Instalación automática.
- Declaración del instalador.
- Configuración de banderas de instalación (pueden heredar configuraciones de Perfil).
- Instalación para un usuario específico / todos los usuarios.
- Dex2oat después de una instalación exitosa.
- Bloqueo de instalación de aplicaciones específicas mediante nombre de paquete o por sharedUID.
- Eliminación automática de APK después de la instalación.
- Sin comandos shell, solo llamadas a API nativas.

## Versiones compatibles

- **Soporte completo:** Android SDK 34 - 36.1 (Android 14 - 16)
- **Soporte limitado:** Android SDK 26 - 33 (Android 8.0 - 13) (por favor, reporta problemas).

## Cambios clave y características

- **Interfaz renovada:** Alternable entre un nuevo diseño de interfaz basado en Material 3 Expressive y Miuix similar a HyperOS.
- **Más personalización:** Más configuraciones de interfaz personalizables.
- **Correcciones de errores:** Resueltos problemas de eliminación de APK del proyecto original en ciertos sistemas.
- **Rendimiento:** Velocidad de análisis optimizada, parsing mejorado de varios tipos de paquetes.
- **Soporte multilingüe:** Más idiomas soportados. ¡Se aceptan contribuciones para más idiomas!
- **Optimización de diálogo:** Visualización mejorada del diálogo de instalación.
- **Iconos del sistema:** Soporte para mostrar paquetes de iconos del sistema durante la instalación. Permite alternar entre iconos de APK y paquetes de iconos del sistema mediante un interruptor.
- **Comparación de versiones:** Soporte para mostrar la comparación de números de versión en formato de una línea o múltiples líneas.
- **Información SDK:** Diálogos de instalación muestran targetSDK y minSDK en formato de una línea o múltiples líneas.
- **Confirmación de instalación por sesión**: Con la ayuda de [InxLocker](https://github.com/Chimioo/InxLocker), confirmar instalación desde tiendas de apps (Aurora Store, F-Droid, etc) ahora es compatible.
- **Evitar intercepciones:** Shizuku/Root puede evitar restricciones de inicio en cadena de OS personalizados al abrir una App después de la instalación.
    - Actualmente solo funciona para instalación por diálogo.
    - Dhizuku carece de permisos suficientes, por lo que se añadió una opción de cuenta regresiva personalizable para reservar tiempo para la acción de abrir la app.
- **Menú extendido:** Para instalación por diálogo (puede activarse en ajustes):
    - Muestra permisos solicitados por la aplicación.
    - Configuración de InstallFlags (puede heredar configuraciones de Perfil global).
      - **Importante:** Configurar InstallFlags **no garantiza** que siempre funcionen. Algunas opciones podrían representar riesgos de seguridad, dependiendo del sistema.
- **Fuentes preconfiguradas:** Soporte para preconfigurar nombres de paquetes de origen de instalación en configuraciones, permitiendo selección rápida en perfiles y el menú de instalación por diálogo.
- **Instalación desde ZIP:** Soporte para instalar archivos APK dentro de archivos ZIP (solo instalación por diálogo).
    - Permite cantidad ilimitada y admite múltiples archivos ZIP.
    - Permite archivos APK en directorios anidados dentro del ZIP, **no limitado al directorio raíz**.
    - Permite gestionar automáticamente múltiples versiones de un mismo paquete mediante deduplicación y selección optimizada.
- **Instalación en lote:** Soporte para instalar múltiples APKs a la vez (selección múltiple y compartir con InstallerX).
    - Solo instalación por diálogo.
    - Sin límite de cantidad.
    - Solo archivos APK.
    - Admite el manejo automático de múltiples versiones del mismo paquete (eliminación de duplicados y selección inteligente).
- **Archivos APKS/APKM/XAPK:** Soporte para selección automática de la mejor división.
    - Permite tanto notificación como instalación por diálogo.
        - Al hacer clic en "Instalar" en la notificación, se selecciona la mejor opción y se continúa con la instalación.
        - En el diálogo, la mejor opción está seleccionada por defecto, pero se puede elegir manualmente.
    - La interfaz de selección de divisiones muestra descripciones amigables para el usuario.
- **Soporte de arquitectura:** Permite instalar paquetes armeabi-v7a en sistemas solo arm64-v8a. La funcionalidad real depende de que el sistema proporcione traducción en tiempo de ejecución.
- **Downgrade con o sin datos:** Soporte para realizar downgrades de apps con o sin preservación de datos en algunos sistemas OEM Android 15.
    - Esta característica solo admite Android 15. En Android 14 versiones anteriores, prueba la opción `Permitir downgrade` en las opciones de instalación.
    - La característica está disponible en las sugerencias inteligentes del diálogo de instalación. Para usarla, activa primero la opción `Mostrar sugerencias inteligentes`.
    - **¡Usa esta característica con extrema precaución en apps del sistema!** La pérdida de datos de una app del sistema podría dejar el dispositivo inutilizable.
    - No compatible con OneUI 7.0, RealmeUI y algunas versiones de ColorOS (restricciones de AOSP). Si solo ves la opción de downgrade *sin* preservación de datos, significa que tu sistema no permite downgrade *con* datos.
- **Lista negra:** Soporte para configurar una lista de nombres de paquetes prohibidos para instalación en las configuraciones.
    - Lista negra mediante nombre de paquete o sharedUID con exenciones.
    - `Permitir una vez` en sugerencias inteligentes.
- **DexOpt:** Después de una instalación exitosa, la app puede realizar automáticamente dex2oat en las aplicaciones instaladas según las configuraciones de Perfil.
    - No admite Dhizuku.
- **Verificación de firma:** Verifica la firma de la app instalada y el APK a instalar, y da una advertencia si no coinciden.
- **Seleccionar usuario objetivo:** Soporte para instalar apps en un usuario específico.
    - Obtiene dinámicamente los detalles del usuario actual.
    - No admite Dhizuku.
    - Puede ser sobrescrito por la opción de instalación `Instalar para todos los usuarios`.
- **Declarar como desinstalador:** Aceptar intentos de desinstalación en ciertos OS; OS personalizados podrían no ser compatibles.
- [Experimental] **Instalación directa desde enlace de descarga:** La versión online permite compartir directamente el enlace de descarga de un archivo APK a InstallerX para instalación. Actualmente, el APK no se mantiene localmente, pero se añadirá una opción para retener el paquete de instalación en el futuro.

## Preguntas frecuentes (FAQ)

> [!NOTE]
> Por favor, lee el FAQ antes de proporcionar feedback.
> Al proporcionar feedback, especifica tu marca de teléfono, versión del sistema, versión del software y operación en detalle.

- **¿Dhizuku no funciona correctamente?**
    - El soporte para **Dhizuku oficial** es limitado. Probado en AVDs con SDK ≥34. El funcionamiento en SDK <34 no está garantizado.
    - Al usar `OwnDroid`, la función `Eliminar automáticamente después de la instalación` podría no funcionar correctamente.
    - En ROMs chinas, errores ocasionales suelen deberse a restricciones del sistema en el funcionamiento en segundo plano de Dhizuku. Se recomienda reiniciar la app de Dhizuku primero.
    - Dhizuku tiene permisos limitados. Muchas operaciones no son posibles (como evitar interceptores de intents del sistema o especificar el origen de instalación). Se recomienda usar Shizuku si es posible.

- **¿No puedes bloquear InstallerX como instalador predeterminado?**
    - Algunos sistemas tienen políticas muy estrictas sobre instaladores de paquetes. Debes usar un módulo LSPosed para interceptar el intent y reenviarlo al instalador en este caso.
    - Funciona mejor con: [Chimioo/InxLocker](https://github.com/Chimioo/InxLocker)  
    - El uso de otros lockers basados en LSPosed ya no es aconsejable.

- ¿Ocurrió un error en la fase de resolución: `No Content Provider` o `reading provider` reportó `Permission Denial`?
    - Has activado la lista de apps ocultas o funciones similares; por favor, configura la lista blanca.

- **HyperOS muestra error "Instalar apps del sistema requiere declarar un instalador válido"**
    - Es una restricción de seguridad del sistema. Debes declarar un instalador que sea una app del sistema (recomendado: `com.android.fileexplorer` o `com.android.vending` para HyperOS; tienda de apps para Vivo).
    - Funciona con Shizuku/Root. **Dhizuku no es compatible**.
    - Nueva característica: InstallerX detecta automáticamente HyperOS y añade una configuración predeterminada (`com.miui.packageinstaller`). Puedes cambiarla en las configuraciones si es necesario.

- **HyperOS reinstala el instalador predeterminado / el bloqueo falla**
    - Intenta habilitar `Bloqueo automático del instalador` en los ajustes.
    - En algunas versiones de HyperOS, el fallo en el bloqueo es esperado.
    - HyperOS intercepta solicitudes de instalación USB (ADB/Shizuku) con un diálogo. Si el usuario rechaza la instalación de una app nueva, el sistema revocará la configuración del instalador y forzará el predeterminado. Si esto ocurre, bloquea InstallerX de nuevo.

- **La barra de progreso de la notificación se congela**
    - Algunos OS personalizados tienen controles muy estrictos de apps en segundo plano. Configura "Sin restricciones en segundo plano" para la app si lo encuentras.
    - La app está optimizada: finaliza todos los servicios en segundo plano y se cierra 1 segundo después de completar la tarea de instalación (cuando el usuario hace clic en "Listo" o limpia la notificación). Puedes activar la notificación de servicio en primer plano para monitorear.

- **¿Problemas en sistemas Oppo/Vivo/Lenovo/... ?**
    - No tenemos dispositivos de estas marcas para pruebas. Puedes discutirlo en [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions), o reportar a través de nuestro [Canal de Telegram](https://t.me/installerx_revived).
    - Para bloquear el instalador en Oppo/Vivo, usa la herramienta de bloqueo.
    - Para instalar apps a través de Shizuku en dispositivos Honor, desactiva `Monitorizar instalación ADB` en opciones de desarrollador.

## Sobre las versiones

> [!WARNING]
> Las versiones de desarrollo pueden ser inestables y las características pueden cambiarse/eliminarse sin aviso.
> Cambiar canales de compilación puede requerir borrado de datos/reinstalación.

- **Rama `dev`:** Contiene características en desarrollo. Si quieres probarlas, busca las builds CI correspondientes en Github Actions.
- **Rama `main`:** Cuando se fusionan cambios estables desde `dev`, el sistema CI/CD construye y publica automáticamente una nueva versión alpha.
- **Versiones estables:** Publicadas manualmente al finalizar una fase de desarrollo/pruebas. CI/CD las publica automáticamente como release.
- **Sobre permiso de red:** A medida que las caracteristicas se han expandido, se han introducido funciones relacionadas con el uso de red. Sin embargo, muchos usuarios prefieren que el instalador permanezca puramente local sin requerir acceso a la red. Por lo tanto, se ofrecen dos versiones: **online** y **offline**. Ambas versiones comparten el mismo nombre de paquete, código de versión y firma, por lo que no se pueden instalar lado a lado (pero pueden reemplazarse directamente). Por favor, descarga según tus necesidades.
  - **Versión online**: Permite compartir enlaces de descarga directos a InstallerX para instalación. Se pueden añadir más utilidades relacionadas con la red en el futuro, pero el permiso de red **nunca** se usará para propósitos no relacionados con la instalación. Segura de usar.
  - **Versión offline**: No solicita permisos de red en absoluto. Al intentar usar características online, recibirás un mensaje de error claro. Esta versión permanece como un instalador puramente local.

## Sobre la localización

¡Ayúdanos a traducir este proyecto! Puedes contribuir en: https://hosted.weblate.org/engage/installerx-revived/

### Estado de localización

[![Estado de la localización](https://hosted.weblate.org/widget/installerx-revived/strings/multi-auto.svg)](https://hosted.weblate.org/engage/installerx-revived/)

## Licencia

Copyright © [iamr0s](https://github.com/iamr0s) y [colaboradores](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX se lanza actualmente bajo [**GNU General Public License v3 (GPL-3)**](http://www.gnu.org/licenses/gpl-3.0), aunque este compromiso puede cambiar en el futuro. Los mantenedores se reservan el derecho de modificar los términos de la licencia o el estado de código abierto del proyecto.

Si basas tu desarrollo en InstallerX, debes cumplir con los términos de la licencia de código abierto de la versión específica del código fuente que uses como base, independientemente de cambios futuros en el proyecto principal.

## Agradecimientos

Este proyecto usa código de, o se basa en la implementación de, los siguientes proyectos:

- [iamr0s/InstallerX](https://github.com/iamr0s/InstallerX)
- [tiann/KernelSU](https://github.com/tiann/KernelSU)
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)
- [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions)
- [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller)
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)
