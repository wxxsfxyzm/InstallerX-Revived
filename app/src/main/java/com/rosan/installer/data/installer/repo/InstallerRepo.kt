package com.rosan.installer.data.installer.repo

import android.app.Activity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.Closeable

interface InstallerRepo : Closeable {
    val id: String
    var error: Throwable
    var config: ConfigEntity
    var data: List<DataEntity>
    var entities: List<SelectInstallEntity>
    val progress: Flow<ProgressEntity>
    val background: Flow<Boolean>
    val uninstallInfo: MutableStateFlow<UninstallInfo?>

    /**
     * Resolves information for a package to be installed.
     * @param activity The activity to use for resolving.
     */
    fun resolveInstall(activity: Activity)
    fun analyse()
    fun install()

    /**
     * Resolves information for a package to be uninstalled.
     * @param packageName The package name to uninstall.
     */
    fun resolveUninstall(activity: Activity, packageName: String)
    fun uninstall(packageName: String)

    fun background(value: Boolean)

    override fun close()
}