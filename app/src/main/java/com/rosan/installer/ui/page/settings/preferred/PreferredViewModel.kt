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
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class PreferredViewModel(
    private val appDataStore: AppDataStore
) : ViewModel(), KoinComponent {
    // TODO migrate to DataStore instead of SharedPreferences
    //private val context by inject<Context>()
    // private val appSharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE)

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
        }
    }

    private fun init() {
        /*        // sharedPreferences synchronized initialization
                synchronized(this) {
                    if (initialized) return
                    initialized = true
                    val listener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        val authorizer =
                            AuthorizerConverter.revert(sharedPreferences.getString("authorizer", null))
                        val customizeAuthorizer =
                            (if (authorizer == ConfigEntity.Authorizer.Customize) sharedPreferences.getString(
                                "customize_authorizer", null
                            ) else null) ?: ""
                        val installMode =
                            InstallModeConverter.revert(sharedPreferences.getString("install_mode", null))
                        state = state.copy(
                            authorizer = authorizer,
                            customizeAuthorizer = customizeAuthorizer,
                            installMode = installMode
                        )
                    }
                    listener.onSharedPreferenceChanged(appSharedPreferences, null)
                    appSharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                    addCloseable { appSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
                }*/
        // DataStore async initialization
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            appDataStore.getString("authorizer").collectLatest { authorizerStr ->
                val authorizer = AuthorizerConverter.revert(authorizerStr)
                val customizeAuthorizerFlow = appDataStore.getString("customize_authorizer")
                val customizeAuthorizer = if (authorizer == ConfigEntity.Authorizer.Customize) {
                    customizeAuthorizerFlow
                } else {
                    kotlinx.coroutines.flow.flowOf("")
                }
                val installModeFlow = appDataStore.getString("install_mode")
                customizeAuthorizer.collectLatest { customize ->
                    installModeFlow.collectLatest { installModeStr ->
                        val installMode = InstallModeConverter.revert(installModeStr)
                        state = state.copy(
                            authorizer = authorizer,
                            customizeAuthorizer = customize,
                            installMode = installMode
                        )
                    }
                }
            }
        }
    }

    private fun changeGlobalAuthorizer(authorizer: ConfigEntity.Authorizer) {
        /*        appSharedPreferences.edit(true) {
                    putString("authorizer", AuthorizerConverter.convert(authorizer))
                }*/
        viewModelScope.launch {
            appDataStore.putString("authorizer", AuthorizerConverter.convert(authorizer))
        }
    }

    private fun changeGlobalCustomizeAuthorizer(customizeAuthorizer: String) {
        /*        val key = "customize_authorizer"
                appSharedPreferences.edit(true) {
                    if (state.authorizerCustomize) putString(key, customizeAuthorizer)
                    else remove(key)
                }*/
        viewModelScope.launch {
            if (state.authorizerCustomize) {
                appDataStore.putString("customize_authorizer", customizeAuthorizer)
            } else {
                appDataStore.putString("customize_authorizer", "")
            }
        }
    }

    private fun changeGlobalInstallMode(installMode: ConfigEntity.InstallMode) {
        /*        appSharedPreferences.edit(true) {
                    putString("install_mode", InstallModeConverter.convert(installMode))
                }*/
        viewModelScope.launch {
            appDataStore.putString("install_mode", InstallModeConverter.convert(installMode))
        }
    }
}