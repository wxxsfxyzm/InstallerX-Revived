package com.rosan.installer.di

import com.rosan.installer.data.settings.model.room.InstallerRoom
import com.rosan.installer.data.settings.repository.AppRepoImpl
import com.rosan.installer.data.settings.repository.ConfigRepoImpl
import com.rosan.installer.domain.settings.repository.AppRepo
import com.rosan.installer.domain.settings.repository.ConfigRepo
import org.koin.dsl.module

val roomModule = module {
    single {
        InstallerRoom.createInstance()
    }

    single<AppRepo> {
        val roomDatabase by inject<InstallerRoom>()
        AppRepoImpl(roomDatabase.appDao)
    }

    single<ConfigRepo> {
        val roomDatabase by inject<InstallerRoom>()
        ConfigRepoImpl(roomDatabase.configDao)
    }
}