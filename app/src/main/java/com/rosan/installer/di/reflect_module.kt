package com.rosan.installer.di

import com.rosan.installer.data.reflect.model.impl.ReflectRepoImpl
import com.rosan.installer.data.reflect.repo.ReflectRepo
import org.koin.dsl.module

val reflectModule = module {
    single<ReflectRepo> { ReflectRepoImpl() }
}