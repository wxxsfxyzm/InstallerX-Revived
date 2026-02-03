package com.rosan.installer.di

import com.rosan.installer.data.installer.model.impl.InstallerRepoImpl
import com.rosan.installer.data.installer.repo.InstallerRepo
import org.koin.dsl.module

val installerModule = module {
    factory<InstallerRepo> {
        val id = getOrNull<String>()
        InstallerRepoImpl.getOrCreate(id)
    }
}