package com.rosan.installer.ui.page.settings.config.apply

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosan.installer.data.common.util.compatVersionCode
import com.rosan.installer.data.common.util.getCompatInstalledPackages
import com.rosan.installer.data.common.util.hasFlag
import com.rosan.installer.data.settings.model.room.entity.AppEntity
import com.rosan.installer.data.settings.repo.AppRepo
import com.rosan.installer.data.settings.repo.ConfigRepo
import com.rosan.installer.ui.common.ViewContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds

class ApplyViewModel(
    private val configRepo: ConfigRepo, private val appRepo: AppRepo, private val id: Long
) : ViewModel(), KoinComponent {
    private val context by inject<Context>()

    private val packageManager = context.packageManager

    val defaultIcon = packageManager.defaultActivityIcon

    var state by mutableStateOf(
        ApplyViewState()
    )

    fun dispatch(action: ApplyViewAction) {
        when (action) {
            ApplyViewAction.Init -> init()
            ApplyViewAction.LoadApps -> loadApps()
            ApplyViewAction.LoadAppEntities -> collectAppEntities()
            is ApplyViewAction.ApplyPackageName -> applyPackageName(
                action.packageName, action.applied
            )

            is ApplyViewAction.Order -> order(action.type)
            is ApplyViewAction.OrderInReverse -> orderInReverse(action.enabled)
            is ApplyViewAction.SelectedFirst -> selectedFirst(action.enabled)
            is ApplyViewAction.ShowSystemApp -> showSystemApp(action.enabled)
            is ApplyViewAction.ShowPackageName -> showPackageName(action.enabled)
            is ApplyViewAction.Search -> search(action.text)
        }
    }

    private var inited = false

    private fun init() {
        if (inited) return
        inited = true
        loadApps()
        collectAppEntities()
    }

    private var loadAppsJob: Job? = null

    private fun loadApps() {
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(
                apps = state.apps.copy(
                    progress = ViewContent.Progress.Loading
                )
            )
            if (state.apps.data.isNotEmpty()) delay(1.5.seconds)
            val list = packageManager.getCompatInstalledPackages(0).map {
                ApplyViewApp(
                    packageName = it.packageName,
                    versionName = it.versionName,
                    versionCode = it.compatVersionCode,
                    firstInstallTime = it.firstInstallTime,
                    lastUpdateTime = it.lastUpdateTime,
                    isSystemApp = it.applicationInfo!!.flags.hasFlag(ApplicationInfo.FLAG_SYSTEM),
                    label = it.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
                )
            }
            state = state.copy(
                apps = state.apps.copy(
                    data = list, progress = ViewContent.Progress.Loaded
                )
            )
        }
    }

    private var collectAppEntitiesJob: Job? = null

    private fun collectAppEntities() {
        collectAppEntitiesJob?.cancel()
        collectAppEntitiesJob = viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(
                appEntities = state.appEntities.copy(
                    progress = ViewContent.Progress.Loading
                )
            )
            appRepo.flowAll().collect {
                state = state.copy(
                    appEntities = state.appEntities.copy(
                        data = it.filter { it.configId == id },
                        progress = ViewContent.Progress.Loaded
                    )
                )
            }
        }
    }

    private fun applyPackageName(packageName: String?, applied: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = appRepo.findByPackageName(packageName)
            if (applied) {
                if (entity != null) {
                    entity.configId = id
                    appRepo.update(entity)
                } else {
                    appRepo.insert(
                        AppEntity(
                            packageName = packageName, configId = id
                        )
                    )
                }
            } else {
                entity?.let { appRepo.delete(it) }
            }
        }
    }

    private fun order(type: ApplyViewState.OrderType) {
        state = state.copy(orderType = type)
    }

    private fun orderInReverse(enabled: Boolean) {
        state = state.copy(orderInReverse = enabled)
    }

    private fun selectedFirst(enabled: Boolean) {
        state = state.copy(selectedFirst = enabled)
    }

    private fun showSystemApp(enabled: Boolean) {
        state = state.copy(showSystemApp = enabled)
    }

    private fun showPackageName(enabled: Boolean) {
        state = state.copy(showPackageName = enabled)
    }

    private fun search(text: String) {
        state = state.copy(search = text)
    }
}