package com.rosan.installer.data.installer.repo

import android.net.Uri
import com.rosan.installer.data.app.model.entity.DataEntity
import com.rosan.installer.data.installer.model.entity.ProgressEntity
import kotlinx.coroutines.flow.MutableSharedFlow

interface NetworkResolver {
    suspend fun resolve(
        uri: Uri,
        cacheDirectory: String,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ): List<DataEntity>
}