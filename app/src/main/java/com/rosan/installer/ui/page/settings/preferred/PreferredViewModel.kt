package com.rosan.installer.ui.page.settings.preferred

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.model.room.entity.converter.AuthorizerConverter
import com.rosan.installer.data.settings.model.room.entity.converter.InstallModeConverter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PreferredViewModel : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    private val appSharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    var state by mutableStateOf(PreferredViewState())
        private set

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

    private var initialized = false

    private fun init() {
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
        }
    }

    private fun changeGlobalAuthorizer(authorizer: ConfigEntity.Authorizer) {
        appSharedPreferences.edit(true) {
            putString("authorizer", AuthorizerConverter.convert(authorizer))
        }
    }

    private fun changeGlobalCustomizeAuthorizer(customizeAuthorizer: String) {
        val key = "customize_authorizer"
        appSharedPreferences.edit(true) {
            if (state.authorizerCustomize) putString(key, customizeAuthorizer)
            else remove(key)
        }
    }

    private fun changeGlobalInstallMode(installMode: ConfigEntity.InstallMode) {
        appSharedPreferences.edit(true) {
            putString("install_mode", InstallModeConverter.convert(installMode))
        }
    }
}