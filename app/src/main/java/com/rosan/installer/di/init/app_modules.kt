package com.rosan.installer.di.init

import com.rosan.installer.di.deviceCapabilityCheckerModule
import com.rosan.installer.di.engineModule
import com.rosan.installer.di.installerModule
import com.rosan.installer.di.logModule
import com.rosan.installer.di.networkModule
import com.rosan.installer.di.privilegedModule
import com.rosan.installer.di.reflectModule
import com.rosan.installer.di.serializationModule
import com.rosan.installer.di.settingsModule
import com.rosan.installer.di.updateModule
import com.rosan.installer.di.viewModelModule

val appModules = listOf(
    viewModelModule,
    serializationModule,
    installerModule,
    reflectModule,
    settingsModule,
    engineModule,
    networkModule,
    updateModule,
    deviceCapabilityCheckerModule,
    logModule,
    privilegedModule
)
