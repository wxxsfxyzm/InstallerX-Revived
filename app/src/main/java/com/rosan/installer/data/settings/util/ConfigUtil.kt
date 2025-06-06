package com.rosan.installer.data.settings.util

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
 * DataStore 迁移注意事项
 * 由于 DataStore 的读写是异步操作，原本通过 `val` 属性（同步获取）的配置项，迁移后需改为 `suspend fun`（挂起函数）。调用时必须在协程或挂起环境下进行，否则会编译报错。
 *
 * 迁移前（同步）
 * val authorizer = ConfigUtil.globalAuthorizer
 *
 * 迁移后（异步）
 * val authorizer = withContext(Dispatchers.IO) {
 *     ConfigUtil.getGlobalAuthorizer()
 * }
 **/
class ConfigUtil {
    companion object : KoinComponent {
        // SharedPreferences
//        private val context by inject<Context>()
//        private val sharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE)

        // DataStore
        private val appDataStore by inject<AppDataStore>()

        /*        val globalAuthorizer: ConfigEntity.Authorizer
                    get() = AuthorizerConverter.revert(sharedPreferences.getString("authorizer", null))

                val globalCustomizeAuthorizer: String
                    get() = sharedPreferences.getString("customize_authorizer", null) ?: ""

                val globalInstallMode: ConfigEntity.InstallMode
                    get() = InstallModeConverter.revert(sharedPreferences.getString("install_mode", null))*/

        suspend fun getGlobalAuthorizer(): ConfigEntity.Authorizer {
            val str = appDataStore.getString("authorizer", "").first()
            return AuthorizerConverter.revert(str)
        }

        suspend fun getGlobalCustomizeAuthorizer(): String {
            return appDataStore.getString("customize_authorizer", "").first()
        }

        suspend fun getGlobalInstallMode(): ConfigEntity.InstallMode {
            val str = appDataStore.getString("install_mode", "").first()
            return InstallModeConverter.revert(str)
        }

        /*        suspend fun getByPackageName(packageName: String? = null): ConfigEntity {
                    var entity = getByPackageNameInner(packageName)
                    if (entity.authorizer == ConfigEntity.Authorizer.Global)
                        entity = entity.copy(
                            authorizer = globalAuthorizer,
                            customizeAuthorizer = globalCustomizeAuthorizer
                        )
                    if (entity.installMode == ConfigEntity.InstallMode.Global)
                        entity = entity.copy(installMode = globalInstallMode)
                    return entity
                }*/

        suspend fun getByPackageName(packageName: String? = null): ConfigEntity {
            var entity = getByPackageNameInner(packageName)
            if (entity.authorizer == ConfigEntity.Authorizer.Global)
                entity = entity.copy(
                    authorizer = getGlobalAuthorizer(),
                    customizeAuthorizer = getGlobalCustomizeAuthorizer()
                )
            if (entity.installMode == ConfigEntity.InstallMode.Global)
                entity = entity.copy(installMode = getGlobalInstallMode())
            return entity
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
}
