package com.rosan.installer.domain.settings.usecase.settings

import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.core.bitmask.addFlag
import com.rosan.installer.core.bitmask.removeFlag

class ToggleUninstallFlagUseCase(
    private val appSettingsRepo: AppSettingsRepository
) {
    suspend operator fun invoke(flag: Int, enable: Boolean): Int? {
        var disabledMutualExclusionFlag: Int? = null

        appSettingsRepo.updateUninstallFlags { currentFlags ->
            var newFlags = currentFlags

            if (enable) {
                newFlags = newFlags.addFlag(flag)

                if (flag == UninstallFlags.DELETE_ALL_USERS) {
                    if (currentFlags and UninstallFlags.DELETE_SYSTEM_APP != 0) {
                        disabledMutualExclusionFlag = UninstallFlags.DELETE_SYSTEM_APP
                        newFlags = newFlags.removeFlag(UninstallFlags.DELETE_SYSTEM_APP)
                    }
                } else if (flag == UninstallFlags.DELETE_SYSTEM_APP) {
                    if (currentFlags and UninstallFlags.DELETE_ALL_USERS != 0) {
                        disabledMutualExclusionFlag = UninstallFlags.DELETE_ALL_USERS
                        newFlags = newFlags.removeFlag(UninstallFlags.DELETE_ALL_USERS)
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
