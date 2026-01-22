package com.rosan.installer.data.app.util

import android.content.pm.PackageInstaller
import com.rosan.installer.data.reflect.repo.ReflectRepo
import com.rosan.installer.data.reflect.repo.getValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object PackageInstallerUtil : KoinComponent {
    private val reflect = get<ReflectRepo>()

    var PackageInstaller.SessionParams.installFlags: Int
        get() = reflect.getValue(this, "installFlags") ?: 0
        set(value) = reflect.setFieldValue(this, PackageInstaller.SessionParams::class.java, "installFlags", value)

    var PackageInstaller.SessionParams.abiOverride: String?
        get() = reflect.getValue(this, "abiOverride")
        set(value) = reflect.setFieldValue(this, PackageInstaller.SessionParams::class.java, "abiOverride", value)
}