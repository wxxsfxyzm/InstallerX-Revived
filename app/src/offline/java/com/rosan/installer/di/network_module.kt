package com.rosan.installer.di

import com.rosan.installer.data.updater.model.impl.OfflineAppUpdater
import com.rosan.installer.data.updater.model.impl.OfflineUpdateChecker
import com.rosan.installer.data.updater.repo.AppUpdater
import com.rosan.installer.data.updater.repo.UpdateChecker
import org.koin.dsl.module

// Empty network module for offline build
val networkModule = module { }

val updateModule = module {
    single<UpdateChecker> { OfflineUpdateChecker() }
    single<AppUpdater> { OfflineAppUpdater() }
}