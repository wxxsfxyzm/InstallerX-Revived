package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.data.engine.executor.PackageManagerUtil
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.util.addFlag
import com.rosan.installer.util.removeFlag

class ToggleUninstallFlagUseCase(
    private val appSettingsRepo: AppSettingsRepository
) {
    suspend operator fun invoke(flag: Int, enable: Boolean): Int? {
        var disabledMutualExclusionFlag: Int? = null

        appSettingsRepo.updateUninstallFlags { currentFlags ->
            var newFlags = currentFlags

            if (enable) {
                newFlags = newFlags.addFlag(flag)

                if (flag == PackageManagerUtil.DELETE_ALL_USERS) {
                    if (currentFlags and PackageManagerUtil.DELETE_SYSTEM_APP != 0) {
                        disabledMutualExclusionFlag = PackageManagerUtil.DELETE_SYSTEM_APP
                        newFlags = newFlags.removeFlag(PackageManagerUtil.DELETE_SYSTEM_APP)
                    }
                } else if (flag == PackageManagerUtil.DELETE_SYSTEM_APP) {
                    if (currentFlags and PackageManagerUtil.DELETE_ALL_USERS != 0) {
                        disabledMutualExclusionFlag = PackageManagerUtil.DELETE_ALL_USERS
                        newFlags = newFlags.removeFlag(PackageManagerUtil.DELETE_ALL_USERS)
                    }
                }
            } else {
                newFlags = newFlags.removeFlag(flag)
            }
            newFlags
        }

        return disabledMutualExclusionFlag
    }
}