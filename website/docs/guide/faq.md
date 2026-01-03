# FAQ

::: info 📝 Before Reporting Issues
Please read the FAQ below before reporting bugs.
When submitting feedback, please include:
* **Device Brand & Model**
* **System Version** (Android version & ROM version)
* **App Version**
* **Authorization Method** (Shizuku, Dhizuku, or Root)
* **Logs** (captured via logcat or LogFox) if possible.
:::

## How to download artifacts from GitHub Actions?

::: tip 💡 For OwnDroid and other projects
This guide applies to downloading build artifacts from any GitHub repository's Actions workflow runs, including [OwnDroid](https://github.com/BinTianqi/OwnDroid) (Dhizuku implementation) and InstallerX itself.
:::

### Step 1: Access the Workflow Run
1. Navigate to the GitHub Actions page (e.g., `https://github.com/BinTianqi/OwnDroid/actions/runs/18057156437`)
2. If you're not logged in to GitHub, you'll need to **sign in** first (artifacts are only accessible to authenticated users)

### Step 2: Download Artifacts
1. Scroll down to the **"Artifacts"** section at the bottom of the workflow run page
2. Click on the artifact name (e.g., `app-release.apk` or similar) to download it
3. The artifact will download as a **ZIP file**

### Step 3: Extract and Install
1. Extract the downloaded ZIP file to access the APK inside
2. Use **InstallerX** to install the APK:
   * Open the APK with InstallerX (via file manager "Open with")
   * Share the APK to InstallerX
   * Or enable "Direct installation from download link" feature in online version

::: warning ⚠️ Important Notes
* GitHub Actions artifacts are only available for **90 days** after the workflow run
* You must be **logged in** to GitHub to download artifacts
* Some workflow runs may require you to have access to the repository
* When using **OwnDroid** with InstallerX, note that the `Auto delete after installation` function might not work correctly (this is a known limitation)
:::

## Dhizuku is not working?
* Please update **Dhizuku** to the latest version.
* On customized Chinese ROMs, random errors are often caused by the system killing Dhizuku's background process. Please try **restarting the Dhizuku app**.
* Dhizuku has limited permissions compared to Shizuku. It cannot perform advanced operations like bypassing system intent interception or specifying installation sources. We recommend using **Shizuku** if possible.
* **Using OwnDroid?** See the guide above for downloading OwnDroid builds from GitHub Actions.

## Cannot lock the installer?
* Some systems strictly restrict 3rd-party installers. You need to use an **LSPosed module** to intercept the intent and forward it to InstallerX.
* We recommend using it with **[InxLocker](https://github.com/Chimioo/InxLocker)** for the best experience.
* Other locker modules are no longer recommended.

## Error: `No Content Provider` or `Permission Denial` during analysis?
* This happens if you are using **Hide My Applist (HMA)** or similar modules.
* Please configure the whitelist to allow InstallerX to access the necessary providers.

## HyperOS/Vivo error: "Installation requires a valid installer declaration"?
* This is a system security restriction. You need to declare the installer package name in the settings.
    * **HyperOS:** Recommend `com.android.fileexplorer` or `com.android.vending`.
    * **Vivo:** Recommend the Vivo App Store package name.
* **Shizuku/Root** works fine; **Dhizuku** does not support this.
* On HyperOS, InstallerX automatically adds a config for `com.miui.packageinstaller` on startup. You can change this in Settings.
* If **Smart Suggestions** is enabled, you can click the suggested option to bypass this restriction upon failure.

## HyperOS: Installer lock reverts to default automatically?
* Try enabling **"Auto Lock Installer"** in Settings.
* On some HyperOS versions, locking is simply not possible due to system constraints.
* HyperOS intercepts USB installation requests (ADB/Shizuku) via a dialog. If you click "Deny" when installing a new app, the system might revoke the installer preference and force it back to default. If this happens, please re-lock it.

## Notification progress bar gets stuck?
* Some customized systems manage background processes very aggressively. Please set the **Background Battery Usage** for InstallerX to **"No restrictions"** (Unrestricted).
* The app is optimized for background management. It will automatically clean up background services and exit **1 second** after the installation task is complete (user clicks finish or clears notification). Enabling "No restrictions" will **not** cause extra battery drain.

## Issues on Oppo / Vivo / Lenovo / etc.?
* We do not have devices from these brands for testing. Please visit [Discussions](https://github.com/wxxsfxyzm/InstallerX-Revived/discussions) or our [Telegram Channel](https://t.me/installerx_revived) for help.
    * **Oppo/Vivo:** You likely need a locker module to replace the system installer.
    * **Honor:** When using Shizuku, disable **"Monitor ADB Installation"** in Developer Options, otherwise installation will hang.

## How to install Magisk/KernelSU modules?
1. Go to **Settings -> Laboratory** and enable **"Enable Module Flashing"**.
2. Open the ZIP file using InstallerX (via file manager "Open with" or "Share to").

## How to replace the System Package Manager?
* **ColorOS:** Change the package name to `com.android.packageinstaller` and flash it as a Magisk module.
* **AOSP / Near-AOSP:** Besides changing the package name, you must add permissions to `/system/etc/permissions/privapp-permissions-platform.xml`. See [this issue comment](https://github.com/wxxsfxyzm/InstallerX-Revived/issues/349#issuecomment-3621922034).
* **Note:** When running as the system package manager, custom settings (like specific installation sources) may **NOT be supported**.