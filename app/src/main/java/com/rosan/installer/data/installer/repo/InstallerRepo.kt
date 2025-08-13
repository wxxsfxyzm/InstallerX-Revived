package com.rosan.installer.data.installer.repo

import android.app.Activity
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import com.rosan.installer.data.installer.model.entity.SelectInstallEntity
import com.rosan.installer.data.installer.model.entity.UninstallInfo
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface InstallerRepo : Closeable {
    val id: String
    var error: Throwable
    var config: ConfigEntity
    var data: List<DataEntity>
    var entities: List<SelectInstallEntity>
    val progress: Flow<ProgressEntity>
    val background: Flow<Boolean>
    val uninstallInfo: Flow<UninstallInfo?>

    fun resolve(activity: Activity)
    fun analyse()
    fun install()
    fun uninstall(packageName: String)
    fun background(value: Boolean)

    override fun close()
}