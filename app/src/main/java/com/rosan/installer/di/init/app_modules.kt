package com.rosan.installer.di.init

import com.rosan.installer.di.appIconModule
import com.rosan.installer.di.datastoreModule
import com.rosan.installer.di.deviceCapabilityCheckerModule
import com.rosan.installer.di.iconColorExtractorModule
import com.rosan.installer.di.installerModule
import com.rosan.installer.di.logModule
import com.rosan.installer.di.networkModule
import com.rosan.installer.di.reflectModule
import com.rosan.installer.di.roomModule
import com.rosan.installer.di.serializationModule
import com.rosan.installer.di.updateModule
import com.rosan.installer.di.viewModelModule

val appModules = listOf(
    roomModule,
    viewModelModule,
    serializationModule,
    installerModule,
    reflectModule,
    datastoreModule,
    appIconModule,
    iconColorExtractorModule,
    networkModule,
    updateModule,
    deviceCapabilityCheckerModule,
    logModule
)
