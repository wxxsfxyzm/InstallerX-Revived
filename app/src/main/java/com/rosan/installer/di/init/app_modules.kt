package com.rosan.installer.di.init

import com.rosan.installer.di.appIconModule
import com.rosan.installer.di.datastoreModule
import com.rosan.installer.di.iconColorExtractorModule
import com.rosan.installer.di.installerModule
import com.rosan.installer.di.paRepoModule
import com.rosan.installer.di.reflectModule
import com.rosan.installer.di.roomModule
import com.rosan.installer.di.serializationModule
import com.rosan.installer.di.viewModelModule

val appModules = listOf(
    roomModule,
    viewModelModule,
    serializationModule,
    //workerModule,
    installerModule,
    paRepoModule,
    reflectModule,
    datastoreModule,
    appIconModule,
    iconColorExtractorModule
)
