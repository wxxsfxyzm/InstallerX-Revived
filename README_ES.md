# InstallerX Revived (Edición Comunitaria)

[![Licencia: GPL v3](https://img.shields.io/badge/Licencia-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Última versión](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Estable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Vista previa](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Pruebas)](https://github.com/wxxsfxyzm/InstallerX/releases)[![Telegram](https://img.shields.io/badge/Telegram-2CA5E0?logo=telegram&logoColor=white)](https://t.me/installerx_revived)

- Esta es una versión mantenida por la comunidad. El [proyecto original](https://github.com/iamr0s/InstallerX) fue archivado por el autor.
- Se ofrecen actualizaciones y soporte limitados en código abierto.
- Este fork sigue estrictamente la GNU GPLv3. Todas las modificaciones son de código abierto.
- ¡Damos la bienvenida a las contribuciones de la comunidad!

## Introducción

> Un instalador de aplicaciones Android moderno y funcional. (Sabes que algunos pájaros no están hechos para estar enjaulados, sus plumas son demasiado brillantes).

¿Necesitas un instalador de apps más potente? Prueba **InstallerX**.

En sistemas modificados (especialmente ROMs chinas), el instalador predeterminado suele ofrecer una experiencia deficiente. Puedes usar **InstallerX** para reemplazarlo.

Además, comparado con instaladores del sistema, **InstallerX** ofrece más opciones de instalación:
- Instalación por diálogo
- Instalación por notificación
- Instalación automática
- Declaración del instalador
- Opción para instalar en el espacio de todos los usuarios
- Permitir paquetes de prueba
- Permitir instalación con downgrade
- Eliminación automática del paquete después de la instalación.

## Versiones compatibles

- **Compatibilidad total:** Android SDK 34 - 36 (Android 14 - 16)
- **Soporte limitado:** Android SDK 26 - 33 (Android 8.0 - 13) (Si tienes problemas, puedes reportarlos en el apartado de *issues*)

## Cambios y características principales

- **Interfaz renovada:** [En pruebas] Interfaz que permite **alternar** entre el diseño clásico y un nuevo diseño basado en Material 3 Expressive.
- **Más personalización:** Más ajustes de interfaz personalizables.
- **Corrección de errores:** se ha solucionado el problema que impedía eliminar correctamente los paquetes de instalación en ciertos sistemas.
- **Rendimiento:** Velocidad de análisis optimizada, con mejor soporte para varios tipos de paquetes de instalación.
- **Soporte multilingüe:** textos ajustados para inglés, chino tradicional y español. ¡Se aceptan contribuciones para más idiomas a través de PR!
- **Optimización de diálogos:** Mejorada la visualización durante la instalación.
- **Iconos del sistema:** Soporte para mostrar iconos de paquetes del sistema durante la instalación (método de [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku/blob/master/manager/src/main/java/moe/shizuku/manager/utils/AppIconCache.kt)).
- **Comparación de versiones:** Soporte para mostrar la comparación de números de versión en una sola línea o en múltiples líneas.
- **Información del SDK:** Los diálogos de instalación muestran el targetSDK y minSDK.
- **Evitar bloqueos:** Shizuku/Root puede evitar las restricciones de lanzamiento en cadena de las interfaces personalizadas al abrir una App después de la instalación.
    - Implementado usando APIs nativas, no comandos shell.
    - Actualmente solo funciona para la instalación por diálogo.
    - Dhizuku no puede invocar permisos, por lo que se añadió una opción de cuenta regresiva personalizable para reservar tiempo a la acción de abrir la app.
- **Menú extendido:** Para la instalación por diálogo (activar en ajustes):
    - Visualización de permisos requeridos por la aplicación.
    - Configuración de InstallFlags (puede heredar la configuración del Perfil global). Parte del código basado en [zacharee/InstallWithOptions](https://github.com/zacharee/InstallWithOptions/blob/main/app/src/main/java/dev/zwander/installwithoptions/data/InstallOption.kt).
      - **Nota importante:** Configurar InstallFlags **no garantiza** que siempre funcionen. Algunas opciones podrían conllevar riesgos de seguridad, dependiendo del sistema.
- **Fuentes preestablecidas:** Soporte para preconfigurar nombres de paquetes de origen de instalación en los ajustes, permitiendo su selección rápida en perfiles y en el menú de instalación por diálogo.
- **Instalación desde ZIP:** Soporte para instalar archivos APK dentro de archivos ZIP (solo instalación por diálogo).
    - Sin límite de cantidad.
    - Soporta archivos APK en directorios anidados dentro del ZIP, **no solo en el directorio raíz**.
    - Soporta el manejo automático de múltiples versiones del mismo paquete:
        - Eliminación de duplicados.
        - Selección inteligente del mejor paquete para instalar.
- **Instalación por lotes:** Soporte para instalar múltiples APKs a la vez (selección múltiple y compartir con InstallerX).
    - Solo instalación por diálogo.
    - Sin límite de cantidad.
    - Solo archivos APK.
    - Soporta el manejo automático de múltiples versiones del mismo paquete (eliminación de duplicados y selección inteligente).
- **Archivos APKS/APKM/XAPK:** Soporte para la selección automática de la mejor división (split). Parte de la idea y código de [vvb2060/PackageInstaller](https://github.com/vvb2060/PackageInstaller/tree/master/app).
    - Soporta tanto instalación por notificación como por diálogo.
        - Al hacer clic en "Instalar" en la notificación se elige la mejor opción.
        - En el diálogo, la mejor opción está seleccionada por defecto, pero se puede elegir manualmente.
    - La interfaz de selección de divisiones muestra descripciones comprensibles para el usuario.
- [En pruebas] **Compatibilidad con arquitecturas**: permite instalar paquetes armeabi-v7a en sistemas arm64-v8a. El funcionamiento depende de que el sistema incluya traducción en tiempo de ejecución.
- [En pruebas] **Downgrade con/sin datos:** Soporte para realizar downgrade de aplicaciones conservando o no los datos en algunos sistemas OEM con Android 15.
    - Esta función solo es compatible con Android 14 y superior. En Android 14, se debe probar primero la opción `Permitir downgrade` en los ajustes de instalación, y si falla, usar esta sugerencia.
    - La función está disponible en las sugerencias inteligentes del diálogo de instalación. Para usarla, activa primero la opción `Mostrar sugerencias inteligentes (Experimental)`.
    - **¡Usa esta función con extrema precaución en aplicaciones del sistema!** La pérdida de datos de apps del sistema podría dejar el dispositivo inutilizable.
    - No compatible con OneUI 7.0, RealmeUI y algunas versiones de ColorOS (restricciones del OEM). Si solo ves la opción de downgrade *sin* conservar datos, significa que tu sistema no soporta el downgrade *con* datos.
- [En pruebas] **Lista negra:** Soporte para configurar una lista de nombres de paquetes prohibidos para su instalación en los ajustes.
    - En desarrollo. Actualmente solo permite añadidos manuales. En el futuro se incluirá una lista predeterminada basada en el modelo de dispositivo (útil para evitar la instalación incorrecta de software de sistema de otros modelos en HyperOS).

## Preguntas frecuentes

- **¿Dhizuku no funciona correctamente?**
    - El soporte para **Dhizuku oficial** es limitado. Se ha probado en AVDs con SDK ≥34. El funcionamiento en SDK <34 no está garantizado.
    - Al usar `OwnDroid`, la función `Eliminar automáticamente después de instalar` podría no funcionar correctamente.
    - En ROMs chinas, los errores ocasionales suelen deberse a que el sistema restringe el funcionamiento en segundo plano de Dhizuku. Se recomienda reiniciar la app de Dhizuku primero.
    - Dhizuku tiene permisos limitados. Muchas operaciones no son posibles (como evitar interceptores de intents del sistema o especificar el origen de la instalación). Se recomienda usar Shizuku si es posible.

- **¿El bloqueador no funciona?**
    - Debido al cambio de nombre del paquete, es necesario usar la versión modificada del [InstallerX Lock Tool](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/main/InstallerX%E9%94%81%E5%AE%9A%E5%99%A8_1.3.apk) de este repositorio.

- **HyperOS muestra error "Instalar apps del sistema requiere declarar un instalador válido"**
    - Es una restricción de seguridad del sistema. Debes declarar un instalador que sea una app del sistema (se recomienda `com.android.fileexplorer` o `com.android.vending`).
    - Funciona con Shizuku/Root. **Dhizuku no es compatible**.
    - Nueva función: InstallerX detecta automáticamente HyperOS y agrega una configuración predeterminada (`com.miui.packageinstaller`). Puedes cambiarla en los ajustes si es necesario.

- **HyperOS reinstala el instalador predeterminado / el bloqueo falla**
    - HyperOS a veces revoca la configuración del instalador del usuario (motivo bajo investigación).
    - En algunas versiones de HyperOS, es normal que el bloqueo falle.
    - HyperOS intercepta las solicitudes de instalación por USB (ADB/Shizuku) con un diálogo. Si el usuario rechaza la instalación de una app nueva, el sistema revocará la configuración del instalador y forzará el predeterminado. Si esto ocurre, vuelve a bloquear InstallerX.

- **La barra de progreso de la notificación se congela en HyperOS**
    - HyperOS tiene controles muy estrictos de aplicaciones en segundo plano. Configura "Sin restricciones en segundo plano" para la app.
    - La app está optimizada: finaliza todos los servicios en segundo plano y se cierra 0.5 segundos después de completar la tarea de instalación (cuando el usuario hace clic en "Listo" o limpia la notificación). Puedes activar la notificación del servicio en primer plano para monitorear.

- **¿Problemas en sistemas Oppo/Vivo/Lenovo/...?**
    - No disponemos de dispositivos de estas marcas para pruebas. Puedes debatirlo en [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions).
    - Para bloquear el instalador en Oppo/Vivo, usa la herramienta de bloqueo (Lock Tool).

## Sobre las versiones

> [!WARNING]
> Las versiones en desarrollo no garantizan estabilidad y pueden añadir/eliminar funciones en cualquier momento.
> Al cambiar entre canales de compilación, podría ser necesario borrar datos o reinstalar la app.

- **Rama `dev`:** Contiene funciones en desarrollo. Si quieres probarlas, busca las builds correspondientes en [Pull Requests](https://github.com/wxxsfxyzm/InstallerX-Revived/pulls).
  - Los cambios de cada commit se detallan en los PRs (pueden estar generados por IA).
- **Rama `main`:** Al fusionarse los cambios estables desde `dev`, el sistema CI/CD construye y publica automáticamente una nueva versión alpha.
- **Versiones estables:** Se publican manualmente al finalizar una fase de desarrollo y aumentar el `versionCode`. El CI/CD las publica automáticamente como release.

## Licencia

Copyright © [iamr0s](https://github.com/iamr0s) y [colaboradores](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX se publica actualmente bajo [**GNU General Public License v3 (GPL-3)**](http://www.gnu.org/copyleft/gpl.html), aunque este compromiso podría cambiar en el futuro. Los mantenedores se reservan el derecho de modificar los términos de licencia o el estado de código abierto del proyecto.

Si basas tu desarrollo en InstallerX, debes cumplir con los términos de la licencia de código abierto de la versión específica del código fuente que uses como base, independientemente de los cambios futuros que se realicen en el proyecto principal.