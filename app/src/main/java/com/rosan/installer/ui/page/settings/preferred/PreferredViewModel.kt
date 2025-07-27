package com.rosan.installer.ui.page.settings.preferred

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.settings.model.datastore.AppDataStore
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.model.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.model.room.entity.converter.InstallModeConverter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class PreferredViewModel(
    private val appDataStore: AppDataStore
) : ViewModel(), KoinComponent {

    var state by mutableStateOf(PreferredViewState())
        private set

    private var initialized = false

    fun dispatch(action: PreferredViewAction) {
        when (action) {
            is PreferredViewAction.Init -> init()
            is PreferredViewAction.ChangeGlobalAuthorizer -> changeGlobalAuthorizer(action.authorizer)
            is PreferredViewAction.ChangeGlobalCustomizeAuthorizer -> changeGlobalCustomizeAuthorizer(
                action.customizeAuthorizer
            )

            is PreferredViewAction.ChangeGlobalInstallMode -> changeGlobalInstallMode(action.installMode)
            is PreferredViewAction.ChangeShowDialogInstallExtendedMenu -> changeShowDialogInstallExtendedMenu(
                action.showMenu
            )

            is PreferredViewAction.ChangeShowIntelligentSuggestion -> changeShowIntelligentSuggestion(
                action.showIntelligentSuggestion
            )

            is PreferredViewAction.ChangeShowDisableNotificationForDialogInstall -> changeShowDisableNotificationForDialogInstall(
                action.showDisableNotification
            )

            is PreferredViewAction.ChangeShowDialogWhenPressingNotification -> changeShowDialogWhenPressingNotification(
                action.showDialog
            )

            is PreferredViewAction.ChangeDhizukuAutoCloseCountDown -> changeDhizukuAutoCloseCountDown(
                action.countDown
            )
        }
    }

    private fun init() {
        // DataStore async initialization
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val authorizerFlow = appDataStore.getString(AppDataStore.AUTHORIZER)
                .map { AuthorizerConverter.revert(it) }
            val customizeAuthorizerFlow = appDataStore.getString(AppDataStore.CUSTOMIZE_AUTHORIZER)
            val installModeFlow = appDataStore.getString(AppDataStore.INSTALL_MODE)
                .map { InstallModeConverter.revert(it) }
            val showDialogInstallExtendedMenuFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU)
            val showIntelligentSuggestionFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, false)
            val showNotificationForDialogInstallFlow =
                appDataStore.getBoolean(AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS, false)
            val showDialogWhenPressingNotificationFlow =
                appDataStore.getBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, true)
            val dhizukuAutoCloseCountDownFlow =
                appDataStore.getInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, 3)

            combine(
                authorizerFlow,
                customizeAuthorizerFlow,
                installModeFlow,
                showDialogInstallExtendedMenuFlow,
                showIntelligentSuggestionFlow,
                showNotificationForDialogInstallFlow,
                showDialogWhenPressingNotificationFlow,
                dhizukuAutoCloseCountDownFlow
            ) { values: Array<Any?> ->
                val authorizer = values[0] as ConfigEntity.Authorizer
                val customize = values[1] as String
                val installMode = values[2] as ConfigEntity.InstallMode
                val showMenu = values[3] as Boolean
                val showIntelligentSuggestion = values[4] as Boolean
                val showNotification = values[5] as Boolean
                val showDialog = values[6] as Boolean
                val countDown = values[7] as Int
                val customizeAuthorizer =
                    if (authorizer == ConfigEntity.Authorizer.Customize) customize else ""
                PreferredViewState(
                    authorizer = authorizer,
                    customizeAuthorizer = customizeAuthorizer,
                    installMode = installMode,
                    showDialogInstallExtendedMenu = showMenu,
                    showIntelligentSuggestion = showIntelligentSuggestion,
                    disableNotificationForDialogInstall = showNotification,
                    showDialogWhenPressingNotification = showDialog,
                    dhizukuAutoCloseCountDown = countDown,
                )
            }.collectLatest { state = it }
        }
    }

    private fun changeGlobalAuthorizer(authorizer: ConfigEntity.Authorizer) {
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.AUTHORIZER, AuthorizerConverter.convert(authorizer))
        }
    }

    private fun changeGlobalCustomizeAuthorizer(customizeAuthorizer: String) {
        viewModelScope.launch {
            if (state.authorizerCustomize) {
                appDataStore.putString(AppDataStore.CUSTOMIZE_AUTHORIZER, customizeAuthorizer)
            } else {
                appDataStore.putString(AppDataStore.CUSTOMIZE_AUTHORIZER, "")
            }
        }
    }

    private fun changeGlobalInstallMode(installMode: ConfigEntity.InstallMode) {
        viewModelScope.launch {
            appDataStore.putString(AppDataStore.INSTALL_MODE, InstallModeConverter.convert(installMode))
        }
    }

    private fun changeShowDialogInstallExtendedMenu(installExtendedMenu: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_EXTENDED_MENU, installExtendedMenu)
        }
    }

    private fun changeShowIntelligentSuggestion(showIntelligentSuggestion: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_SHOW_INTELLIGENT_SUGGESTION, showIntelligentSuggestion)
        }
    }

    private fun changeShowDisableNotificationForDialogInstall(showDisableNotification: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.DIALOG_DISABLE_NOTIFICATION_ON_DISMISS, showDisableNotification)
        }
    }

    private fun changeShowDialogWhenPressingNotification(showDialog: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean(AppDataStore.SHOW_DIALOG_WHEN_PRESSING_NOTIFICATION, showDialog)
        }
    }

    private fun changeDhizukuAutoCloseCountDown(countDown: Int) {
        viewModelScope.launch {
            // Ensure countDown is within the valid range
            if (countDown in 1..10) {
                appDataStore.putInt(AppDataStore.DIALOG_AUTO_CLOSE_COUNTDOWN, countDown)
            }
        }
    }
}