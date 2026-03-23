// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.data.updater.provider

import android.content.Context
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.engine.model.DataType
import com.rosan.installer.domain.engine.model.InstallEntity
import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.updater.provider.InAppInstallProvider
import java.io.InputStream

class InAppInstallProviderImpl(
    private val context: Context,
    private val appInstaller: AppInstallerRepository
) : InAppInstallProvider {

    override suspend fun executeInstall(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        config: ConfigModel
    ) {
        // Here we map the pure InputStream back to the Installer's specific DataEntity
        val streamDataEntity = DataEntity.StreamDataEntity(
            stream = inputStream,
            length = contentLength
        )

        val installEntity = InstallEntity(
            name = fileName,
            packageName = context.packageName,
            data = streamDataEntity,
            sourceType = DataType.APK
        )

        appInstaller.doInstallWork(
            config = config,
            entities = listOf(installEntity),
            blacklist = emptyList(),
            sharedUserIdBlacklist = emptyList(),
            sharedUserIdExemption = emptyList()
        )
    }
}
