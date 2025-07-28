# InstallerX Revived (Edición Comunitaria)

[![Licencia: GPL v3](https://img.shields.io/badge/Licencia-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Última versión](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Estable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Vista previa](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Pruebas)](https://github.com/wxxsfxyzm/InstallerX/releases)

- Esta es una versión mantenida por la comunidad. El [proyecto original](https://github.com/iamr0s/InstallerX) fue archivado por el autor.
- Se ofrecen actualizaciones y soporte limitados en código abierto.
- Este fork sigue estrictamente la GNU GPLv3. Todas las modificaciones son de código abierto.

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
- Eliminación automática del paquete después de instalar

## Versiones compatibles

- **Compatibilidad total:** Android SDK 34 - 36
- **Soporte limitado:** Android SDK 30 - 33 (reportar problemas en *issues*)

## Cambios principales

- **Interfaz renovada:** Diseño basado en Material 3 Expressive
- **Corrección de bugs:** Se solucionó el problema que impedía eliminar correctamente los paquetes de instalación en ciertos sistemas
- **Soporte multilingüe:** Textos ajustados para inglés, chino tradicional y español. ¡Contribuciones para más idiomas son bienvenidas!
- **Optimización de diálogos:** Mejorada la visualización durante la instalación
- **Nuevas funciones:**
  - Muestra de targetSDK y minSDK durante la instalación
  - Soporte para mostrar iconos del sistema (método de [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku))
  - Shizuku/Root puede saltar bloqueos de lanzamiento en sistemas personalizados (solo para instalación por diálogo)
  - Menú extendido en diálogo de instalación (activar en ajustes):
    - Visualización de permisos requeridos
    - Configuración de InstallFlags (basado en [InstallWithOptions](https://github.com/zacharee/InstallWithOptions))
      - **Nota:** Algunas opciones pueden no funcionar o presentar riesgos de seguridad
  - Soporte para instalar APKs desde archivos ZIP (solo diálogo de instalación):
    - Detección semi-inteligente del mejor paquete a instalar

## Preguntas frecuentes

- **Dhizuku no funciona correctamente**
  - Los desarrolladores no usan Dhizuku personalmente y tienen conocimiento limitado sobre él.
  - Dhizuku tiene limitaciones de permisos (no puede saltar bloqueos de intent o especificar origen de instalación)
  - En dispositivos con SDK ≥34 se han realizado pruebas en AVDs
  - Se recomienda usar Shizuku cuando sea posible

- **El bloqueador no funciona**
  - Debido al cambio de nombre del paquete, usa la versión modificada del [InstallerX Lock Tool](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/main/InstallerX%E9%94%81%E5%AE%9A%E5%99%A8_1.3.apk)

- **HyperOS muestra error "Instalar apps del sistema requiere declarar un instalador válido"**
  - Restricción de seguridad del sistema
  - Declara un instalador de sistema (recomendado: `com.android.fileexplorer` o `com.android.vending`)
  - Funciona con Shizuku/Root (Dhizuku no compatible)
  - Nueva función: Detección automática de HyperOS para agregar instalador en la configuración predeterminada

- **HyperOS reinstala el instalador predeterminado**
  - HyperOS revoca los permisos si el usuario rechaza una instalación ADB/Shizuku
  - Solución: Volver a bloquear el instalador manualmente

- **Notificación de instalación se congela en HyperOS**
  - HyperOS tiene controles estrictos de aplicaciones en segundo plano
  - Configura "Sin restricciones en segundo plano"
  - La app se cierra automáticamente 5 segundos después de completar la instalación

- **Problemas en sistemas Oppo/Vivo/Lenovo**
  - No disponemos de dispositivos de estas marcas para pruebas. Puedes debatirlo en [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions).

## Sobre las versiones

> [!WARNING]
> Las versiones en desarrollo no garantizan estabilidad y pueden cambiar/eliminar funciones sin previo aviso.
> Al cambiar canales de compilación, puede ser necesario limpiar datos/reinstalar.

- **Rama `dev`:** Funciones en prueba (ver builds en [Pull Requests](https://github.com/wxxsfxyzm/InstallerX-Revived/pulls))
  - Los cambios se documentan en los PRs (pueden generarse con IA)
- **Versiones alpha:** Automáticas al fusionar en `main`
- **Versiones estables:** Publicadas manualmente al incrementar `versionCode`

## Licencia

Copyright © [iamr0s](https://github.com/iamr0s) y [colaboradores](https://github.com/wxxsfxyzm/InstallerX-Revived/graphs/contributors)

InstallerX se publica actualmente bajo [**GNU GPL v3**](http://www.gnu.org/copyleft/gpl.html), aunque este compromiso podría cambiar en el futuro. Los mantenedores se reservan el derecho de modificar los términos de licencia o incluso cerrar el código.

Si creas aplicaciones basadas en InstallerX, debes cumplir con los términos de la licencia GPL-3.0 de la versión específica que uses como base, independientemente de los cambios futuros que se realicen en el proyecto principal.
