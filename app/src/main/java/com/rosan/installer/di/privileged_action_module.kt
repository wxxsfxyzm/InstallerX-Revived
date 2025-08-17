package com.rosan.installer.di

import com.rosan.installer.data.app.model.impl.PARepoImpl
import com.rosan.installer.data.app.repo.PrivilegedActionRepo
import org.koin.dsl.module

val paRepoModule = module { single<PrivilegedActionRepo> { PARepoImpl } }