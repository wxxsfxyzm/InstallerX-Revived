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
        }
    }

    private fun init() {
        // DataStore async initialization
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val authorizerFlow = appDataStore.getString("authorizer")
                .map { AuthorizerConverter.revert(it) }
            val customizeAuthorizerFlow = appDataStore.getString("customize_authorizer")
            val installModeFlow = appDataStore.getString("install_mode")
                .map { InstallModeConverter.revert(it) }
            val showDialogInstallExtendedMenuFlow =
                appDataStore.getBoolean("show_dialog_install_extended_menu")

            combine(
                authorizerFlow,
                customizeAuthorizerFlow,
                installModeFlow,
                showDialogInstallExtendedMenuFlow
            ) { authorizer, customize, installMode, showMenu ->
                val customizeAuthorizer =
                    if (authorizer == ConfigEntity.Authorizer.Customize) customize else ""
                PreferredViewState(
                    authorizer = authorizer,
                    customizeAuthorizer = customizeAuthorizer,
                    installMode = installMode,
                    showDialogInstallExtendedMenu = showMenu
                )
            }.collectLatest { state = it }
        }
    }

    private fun changeGlobalAuthorizer(authorizer: ConfigEntity.Authorizer) {
        viewModelScope.launch {
            appDataStore.putString("authorizer", AuthorizerConverter.convert(authorizer))
        }
    }

    private fun changeGlobalCustomizeAuthorizer(customizeAuthorizer: String) {
        viewModelScope.launch {
            if (state.authorizerCustomize) {
                appDataStore.putString("customize_authorizer", customizeAuthorizer)
            } else {
                appDataStore.putString("customize_authorizer", "")
            }
        }
    }

    private fun changeGlobalInstallMode(installMode: ConfigEntity.InstallMode) {
        viewModelScope.launch {
            appDataStore.putString("install_mode", InstallModeConverter.convert(installMode))
        }
    }

    private fun changeShowDialogInstallExtendedMenu(installExtendedMenu: Boolean) {
        viewModelScope.launch {
            appDataStore.putBoolean("show_dialog_install_extended_menu", installExtendedMenu)
        }
    }
}