package com.rosan.installer.data.settings.util

import android.content.Context
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.AppEntity
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.model.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.model.room.entity.converter.InstallModeConverter
import com.rosan.installer.data.settings.repo.AppRepo
import com.rosan.installer.data.settings.repo.ConfigRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

/**
 * Utility class for managing configuration settings.
 *
 * DataStore Migration Notes
 * Since DataStore read/write operations are asynchronous, configuration items that were previously
 * accessed synchronously via `val` properties must be migrated to `suspend fun` (suspending functions).
 * Such functions must be called from within a coroutine or a suspending context; otherwise,
 * a compilation error will occur.
 *
 * Before migration (synchronous)
 * ```
 * val authorizer = ConfigUtil.globalAuthorizer
 *```
 * After migration (asynchronous)
 * ```
 * val authorizer = withContext(Dispatchers.IO) {
 *     ConfigUtil.getGlobalAuthorizer()
 * }
 * ```
 **/

object ConfigUtil : KoinComponent {
    private val context by inject<Context>()

    private val appDataStore by inject<AppDataStore>()

    suspend fun getGlobalAuthorizer(): ConfigEntity.Authorizer {
        val str = appDataStore.getString(AppDataStore.AUTHORIZER, "").first()
        return AuthorizerConverter.revert(str)
    }

    suspend fun ConfigEntity.Authorizer.readGlobal() =
        if (this == ConfigEntity.Authorizer.Global)
            getGlobalAuthorizer()
        else
            this

    suspend fun getGlobalCustomizeAuthorizer(): String {
        return appDataStore.getString(AppDataStore.CUSTOMIZE_AUTHORIZER, "").first()
    }

    suspend fun getGlobalInstallMode(): ConfigEntity.InstallMode {
        val str = appDataStore.getString(AppDataStore.INSTALL_MODE, "").first()
        return InstallModeConverter.revert(str)
    }

    suspend fun getByPackageName(packageName: String? = null): ConfigEntity {
        var entity = getByPackageNameInner(packageName)

        // Handle Global overrides for Authorizer and InstallMode
        if (entity.authorizer == ConfigEntity.Authorizer.Global)
            entity = entity.copy(
                authorizer = getGlobalAuthorizer(),
                customizeAuthorizer = getGlobalCustomizeAuthorizer()
            )
        if (entity.installMode == ConfigEntity.InstallMode.Global)
            entity = entity.copy(installMode = getGlobalInstallMode())

        // Apply runtime properties
        return entity.apply {
            // Resolve uninstallFlags set by user
            uninstallFlags = appDataStore.getInt(AppDataStore.UNINSTALL_FLAGS, 0).first()
            // Check if the Install Requester feature is enabled in DataStore
            val isRequesterEnabled = appDataStore.getBoolean(AppDataStore.LAB_SET_INSTALL_REQUESTER).first()

            if (isRequesterEnabled) {
                // Try to resolve UID from the custom 'installRequester' defined in ConfigEntity
                var targetUid: Int? = installRequester?.let { requesterPkg ->
                    runCatching {
                        context.packageManager.getPackageUid(requesterPkg, 0)
                    }.getOrNull()
                }

                // Fallback: If 'installRequester' is not set, or the package is not found on device,
                // fall back to the existing logic using the incoming 'packageName'.
                if (targetUid == null) {
                    packageName?.let { pkg ->
                        targetUid = runCatching {
                            context.packageManager.getPackageUid(pkg, 0)
                        }.getOrNull()
                    }
                }

                callingFromUid = targetUid
            }
        }
    }

    private suspend fun getByPackageNameInner(packageName: String? = null): ConfigEntity =
        withContext(Dispatchers.IO) {
            val repo = get<ConfigRepo>()
            val app = getAppByPackageName(packageName)
            var config: ConfigEntity? = null
            if (app != null) config = repo.find(app.configId)
            if (config != null) return@withContext config
            config = repo.all().firstOrNull()
            if (config != null) return@withContext config
            return@withContext ConfigEntity.default
        }

    private fun getAppByPackageName(packageName: String? = null): AppEntity? {
        val repo = get<AppRepo>()
        var app: AppEntity? = repo.findByPackageName(packageName)
        if (app != null) return app
        if (packageName != null) app = repo.findByPackageName(null)
        if (app != null) return app
        return null
    }
}