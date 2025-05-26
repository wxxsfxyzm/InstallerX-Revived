package com.rosan.installer.ui.page.settings.config.edit

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.R
import com.rosan.installer.data.settings.model.room.entity.ConfigEntity
import com.rosan.installer.data.settings.repo.ConfigRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EditViewModel(
    private val repo: ConfigRepo,
    private val id: Long? = null
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    var state by mutableStateOf(EditViewState())
        private set

    private val _eventFlow = MutableSharedFlow<EditViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun dispatch(action: EditViewAction) {
        viewModelScope.launch {
            val errorMessage = kotlin.runCatching {
                when (action) {
                    is EditViewAction.Init -> init()
                    is EditViewAction.ChangeDataName -> changeDataName(action.name)
                    is EditViewAction.ChangeDataDescription -> changeDataDescription(action.description)
                    is EditViewAction.ChangeDataAuthorizer -> changeDataAuthorizer(action.authorizer)
                    is EditViewAction.ChangeDataCustomizeAuthorizer -> changeDataCustomizeAuthorizer(
                        action.customizeAuthorizer
                    )
                    is EditViewAction.ChangeDataInstallMode -> changeDataInstallMode(action.installMode)
                    is EditViewAction.ChangeDataDeclareInstaller -> changeDataDeclareInstaller(
                        action.declareInstaller
                    )
                    is EditViewAction.ChangeDataInstaller -> changeDataInstaller(action.installer)
                    is EditViewAction.ChangeDataForAllUser -> changeDataForAllUser(action.forAllUser)
                    is EditViewAction.ChangeDataAllowTestOnly -> changeDataAllowTestOnly(action.allowTestOnly)
                    is EditViewAction.ChangeDataAllowDowngrade -> changeDataAllowDowngrade(action.allowDowngrade)
                    is EditViewAction.ChangeDataAutoDelete -> changeDataAutoDelete(action.autoDelete)
                    is EditViewAction.ChangeDisplaySdk -> changeDisplaySdk(action.displaySdk)
                    is EditViewAction.LoadData -> loadData()
                    is EditViewAction.SaveData -> saveData()
                }
            }.exceptionOrNull()?.message
            if (errorMessage != null) {
                _eventFlow.emit(EditViewEvent.SnackBar(message = errorMessage))
            }
        }
    }

    private var isInited: Boolean = false

    private fun init() {
        synchronized(this) {
            if (isInited) return
            isInited = true
            loadData()
        }
    }

    private fun changeDataName(name: String) {
        if (name.length > 20) return
        if (name.lines().size > 1) return
        state = state.copy(
            data = state.data.copy(
                name = name
            )
        )
    }

    private fun changeDataDescription(description: String) {
        if (description.length > 4096) return
        if (description.lines().size > 8) return
        state = state.copy(
            data = state.data.copy(
                description = description
            )
        )
    }

    private fun changeDataAuthorizer(authorizer: ConfigEntity.Authorizer) {
        state = state.copy(
            data = state.data.copy(
                authorizer = authorizer
            )
        )
    }

    private fun changeDataCustomizeAuthorizer(customizeAuthorizer: String) {
        state = state.copy(
            data = state.data.copy(
                customizeAuthorizer = customizeAuthorizer
            )
        )
    }

    private fun changeDataInstallMode(installMode: ConfigEntity.InstallMode) {
        state = state.copy(
            data = state.data.copy(
                installMode = installMode
            )
        )
    }

    private fun changeDataDeclareInstaller(declareInstaller: Boolean) {
        state = state.copy(
            data = state.data.copy(
                declareInstaller = declareInstaller
            )
        )
    }

    private fun changeDataInstaller(installer: String) {
        state = state.copy(
            data = state.data.copy(
                installer = installer
            )
        )
    }

    private fun changeDataForAllUser(forAllUser: Boolean) {
        state = state.copy(
            data = state.data.copy(
                forAllUser = forAllUser
            )
        )
    }

    private fun changeDataAllowTestOnly(allowTestOnly: Boolean) {
        state = state.copy(
            data = state.data.copy(
                allowTestOnly = allowTestOnly
            )
        )
    }

    private fun changeDataAllowDowngrade(allowDowngrade: Boolean) {
        state = state.copy(
            data = state.data.copy(
                allowDowngrade = allowDowngrade
            )
        )
    }

    private fun changeDataAutoDelete(autoDelete: Boolean) {
        state = state.copy(
            data = state.data.copy(
                autoDelete = autoDelete
            )
        )
    }

    private fun changeDisplaySdk(displaySdk: Boolean) {
        state = state.copy(
            data = state.data.copy(
                displaySdk = displaySdk
            )
        )
    }

    private var loadDataJob: Job? = null

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(
                data = EditViewState.Data.build(id?.let { repo.find(id) } ?: ConfigEntity.default)
            )
        }
    }

    private var saveDataJob: Job? = null

    private fun saveData() {
        saveDataJob?.cancel()
        saveDataJob = viewModelScope.launch(Dispatchers.IO) {
            val message = when {
                state.data.errorName -> context.getString(R.string.config_error_name)
                state.data.errorCustomizeAuthorizer -> context.getString(R.string.config_error_customize_authorizer)
                state.data.errorInstaller -> context.getString(R.string.config_error_installer)
                else -> null
            }
            if (message != null) {
                _eventFlow.emit(EditViewEvent.SnackBar(message = message))
            } else {
                val entity = state.data.toConfigEntity()
                if (id == null) repo.insert(entity)
                else repo.update(entity.also {
                    it.id = id
                })
                _eventFlow.emit(EditViewEvent.Saved)
            }
        }
    }
}