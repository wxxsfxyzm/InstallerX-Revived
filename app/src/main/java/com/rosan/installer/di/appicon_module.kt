package com.rosan.installer.di

import com.rosan.installer.data.app.model.impl.AppIconRepoImpl
import com.rosan.installer.data.app.repo.AppIconRepo
import org.koin.dsl.module

val appIconModule = module {
    single<AppIconRepo> { AppIconRepoImpl() }
}