---
title: App Settings - InstallerX Revived
description: Configure app settings in InstallerX Revived for optimal installation experience
---

# App Settings

App settings provide you with a wealth of customization options, allowing you to tailor the app to your usage habits and preferences.

## Theme

Customize the look and feel of the app.

### UI Engine
InstallerX Revived offers two UI engines for you to choose from:
*   **Material Design 3 (MD3)**: Google's latest design language, providing a modern and dynamic interface experience. By default, it uses the Material 3 Expressive style. If you don't like it, you can revert to the standard MD3 style in the theme settings.
*   **MIUIX**: The design style of Xiaomi's HyperOS, offering a different visual and interactive feel. Please note that the [MIUIX library](https://github.com/compose-miuix-ui/miuix) is under active development and optimization. The UI may change at any time, and you are welcome to provide feedback to me on any issues you encounter.

### Dynamic Theming
The app supports dynamic theming, which works on all Android versions. It extracts colors from your wallpaper and applies them throughout the app's interface, creating a visually harmonious effect that matches your system.

You can further customize color behaviors:
*   **Colorful Dialog**: When enabled, the installation dialog will extract colors from the icon of the app being installed, making each installation interface unique.
*   **Colorful Live Notification Progress**: On Android 16 and newer, this option allows the installation progress bar in live notifications to also follow the app icon's color. Please note that you must first enable the "Live Notification" feature for the app to configure this option.

### Icon Preference
You can choose the source for the icons displayed in the installation dialog:
*   **Parse from Package**: Directly parses and displays the original icon from the APK file.
*   **Use System Icon Pack**: If you have a third-party icon pack installed, you can select this option to make the icons in the app list consistent with your system icons. This is tested working on HyperOS and OneUI.

### Hide App Icon
You can choose to hide InstallerX's launcher icon. Please note that due to system limitations on Android 10+, apps are no longer allowed to hide their desktop icons on Android 10 and above. Therefore, on some systems, after hiding the icon, an icon pointing to the system setting's app details page may still appear on the desktop or in the app drawer. You can disable this restriction in LSPosed's settings. 

---

## Installation Options

Configure global installation behaviors and parameters.

### Global Authorizer
The authorizer set here (e.g., Root or Shizuku) will be used by the app to perform global operations that require privileges, such as locking the default installer, modifying ADB verification settings, etc. User profiles can also inherit this setting to perform installations.

### Global Installation Method
Here, you can select a global default installation method (e.g., Root, Shizuku), which can be inherited by profiles. You can also customize the UI behavior during the installation process.

#### Explanation of Custom Settings
*   **Custom Install Confirmation Dialog**: When enabled, uses the app's built-in dialog instead of the system's default installation confirmation dialog.
*   **Show Toast on Completion**: Displays a brief message at the bottom of the screen after an installation succeeds or fails.
*   **Open App After Installation**: Provides a shortcut button to immediately open the newly installed app upon successful installation.

### Preset Installation Sources
You can preset some frequently used installation sources (app package names). This allows for quick selection when creating a profile for an app or in the installation dialog, simplifying the workflow.

### Blacklist
The blacklist can be used to block the installation of specific applications. You can add rules based on the app's **package name** or **shared UID**. Any application matching a blacklist rule will be intercepted and cannot be installed.

---

## Uninstaller Settings

Configure options related to app uninstallation.

### Authorizer Note
The uninstaller always uses the **Global Authorizer** you set in the "Installation Options" to perform uninstall operations, ensuring it has the necessary permissions.

### Uninstall Options
*   **Keep App Data**: Uninstalls the app but keeps its data and cache files.
*   **Downgrade Installation (Keep Data)**: Downgrades the app to an older version while preserving user data.
*   **Freeze/Unfreeze App**: Temporarily disables or re-enables an app instead of completely uninstalling it.

**Note**: "Downgrade Installation" and "Freeze/Unfreeze App" are mutually exclusive because they are two different operations that cannot be performed simultaneously.

### Non-Root Shortcut
For non-root users, system limitations prevent locking the system uninstaller like root users can. Therefore, InstallerX provides a convenient shortcut: you can simply enter the target app's package name to quickly invoke the system's uninstall process.
