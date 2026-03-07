package com.rosan.installer.ui.page.main.settings.config.all

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.rosan.installer.domain.settings.model.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepo
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.domain.settings.repository.ConfigRepo
import com.rosan.installer.domain.settings.util.ConfigOrder
import com.rosan.installer.ui.page.main.settings.SettingsScreen
import com.rosan.installer.ui.page.miuix.settings.MiuixSettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class AllViewModel(
    var navController: NavController,
    private val repo: ConfigRepo,
    private val appSettingsRepo: AppSettingsRepo
) : ViewModel(), KoinComponent {
    var state by mutableStateOf(AllViewState())
        private set

    private val _eventFlow = MutableSharedFlow<AllViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    fun dispatch(action: AllViewAction) {
        when (action) {
            is AllViewAction.LoadData -> loadData()
            is AllViewAction.UserReadScopeTips -> userReadTips()
            is AllViewAction.ChangeDataConfigOrder -> changeDataConfigOrder(action.configOrder)
            is AllViewAction.DeleteDataConfig -> deleteDataConfig(action.configModel)
            is AllViewAction.RestoreDataConfig -> restoreDataConfig(action.configModel)
            is AllViewAction.EditDataConfig -> editDataConfig(action.configModel)
            is AllViewAction.MiuixEditDataConfig -> editMiuixDataConfig(action.configModel)
            is AllViewAction.ApplyConfig -> applyConfig(action.configModel)
        }
    }

    private var loadDataJob: Job? = null

    private fun loadData() {
        loadDataJob?.cancel()
        state = state.copy(
            data = state.data.copy(
                progress = AllViewState.Data.Progress.Loading
            )
        )
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {
            val initialState = AllViewState(
                userReadScopeTips = appSettingsRepo.getBoolean(BooleanSetting.UserReadScopeTips, default = false).first(),
            )
            repo.flowAll(state.data.configOrder).collect {
                state = state.copy(
                    userReadScopeTips = initialState.userReadScopeTips,
                    data = state.data.copy(
                        configs = it,
                        progress = AllViewState.Data.Progress.Loaded
                    )
                )
            }
        }
    }

    private fun userReadTips() {
        state = state.copy(userReadScopeTips = true)
        viewModelScope.launch {
            appSettingsRepo.putBoolean(BooleanSetting.UserReadScopeTips, true)
        }
    }

    private fun editDataConfig(configModel: ConfigModel) {
        viewModelScope.launch {
            navController.navigate(
                SettingsScreen.Builder.EditConfig(
                    configModel.id
                ).route
            )
        }
    }

    private fun editMiuixDataConfig(configModel: ConfigModel) {
        viewModelScope.launch {
            navController.navigate(
                MiuixSettingsScreen.Builder.MiuixEditConfig(
                    configModel.id
                ).route
            )
        }
    }

    private fun changeDataConfigOrder(configOrder: ConfigOrder) {
        state = state.copy(data = state.data.copy(configOrder = configOrder))
    }

    private fun deleteDataConfig(configModel: ConfigModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(configModel)
            _eventFlow.emit(AllViewEvent.DeletedConfig(configModel))
        }
    }

    private fun restoreDataConfig(configModel: ConfigModel) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.insert(configModel)
        }
    }

    private fun applyConfig(configModel: ConfigModel) {
        viewModelScope.launch {
            navController.navigate(
                SettingsScreen.Builder.ApplyConfig(
                    configModel.id
                ).route
            )
        }
    }
}