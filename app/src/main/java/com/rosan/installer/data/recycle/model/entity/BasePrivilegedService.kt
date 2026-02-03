package com.rosan.installer.data.recycle.model.entity

import android.content.Context
import com.rosan.installer.IPrivilegedService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BasePrivilegedService : IPrivilegedService.Stub(), KoinComponent {
    protected val context by inject<Context>()
}