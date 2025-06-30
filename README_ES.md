# InstallerX Revived (Edición Comunitaria)

[![Licencia: GPL v3](https://img.shields.io/badge/Licencia-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)[![Última versión](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?label=Estable)](https://github.com/wxxsfxyzm/InstallerX/releases/latest)[![Versión preliminar](https://img.shields.io/github/v/release/wxxsfxyzm/InstallerX?include_prereleases&label=Pruebas)](https://github.com/wxxsfxyzm/InstallerX/releases)

- Esta es una versión mantenida por la comunidad. El [proyecto original](https://github.com/iamr0s/InstallerX) fue archivado por el autor.
- Ofrece actualizaciones y soporte limitados en código abierto.
- Esta bifurcación sigue estrictamente la GNU GPLv3. Todas las modificaciones son de código abierto.

## Introducción

> Un instalador de aplicaciones Android moderno y funcional. (Sabes que algunos pájaros no están hechos para estar enjaulados, sus plumas son demasiado brillantes).

¿Necesitas un instalador de apps más potente? Prueba **InstallerX**.

En sistemas modificados (especialmente chinos), el instalador predeterminado suele tener una experiencia deficiente. Puedes usar **InstallerX** para reemplazarlo.

Además, comparado con sistemas nativos, **InstallerX** ofrece más opciones de instalación:
- Instalación por diálogo
- Instalación por notificación
- Instalación automática
- Declaración del instalador
- Opción para instalar en el espacio de todos los usuarios
- Permitir paquetes de prueba
- Permitir instalación con downgrade
- Eliminación automática del paquete después de instalar

## Versiones compatibles

- SDK 34 o superior (compatibilidad total)
- SDK 30-33 (soporte limitado, reportar problemas en *issues*)

## Cambios principales

- Interfaz simplificada
- Corrección de un bug que impedía eliminar correctamente los paquetes de instalación
- Ajustes de texto, ahora con soporte para chino tradicional. ¡PRs para más idiomas son bienvenidos!
- Mejoras en la visualización de versiones de aplicaciones durante la instalación
- Nueva función para mostrar targetSDK y minSDK durante la instalación

## Preguntas frecuentes

- **¿El bloqueador no funciona?**
  - Debido a cambios en el nombre del paquete, debes usar la versión modificada del bloqueador en este repositorio: [InstallerX Lock Tool](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/main/InstallerX%E9%94%81%E5%AE%9A%E5%99%A8_1.3.apk)

- **HyperOS muestra el error** `Instalar apps del sistema requiere declarar un instalador válido` **¿Qué hacer?**
  - Restricción de seguridad del sistema. Debes declarar un instalador de sistema en la configuración (recomendado: `com.android.fileexplorer` o `com.android.vending`).
  - En el futuro se podría añadir: *"Detección automática de HyperOS para agregar instalador en la configuración predeterminada"*.

- **¿No funciona en sistemas Oppo/Vivo/Lenovo?**
  - No disponemos de dispositivos de estas marcas para pruebas. Puedes debatirlo en [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions).

## Licencia

Copyright © [iamr0s](https://github.com/iamr0s) y colaboradores

InstallerX se publica actualmente bajo [**GNU GPL v3**](http://www.gnu.org/copyleft/gpl.html), aunque este compromiso podría cambiar en el futuro. Los mantenedores se reservan el derecho de modificar los términos de licencia o incluso cerrar el código.

Si desarrollas derivados basados en InstallerX, deberás cumplir con los términos GPL-3.0 de la versión específica que utilices como base, independientemente de cambios futuros en el proyecto principal.