package com.rosan.installer.domain.session.repository

import android.net.Uri
import com.rosan.installer.domain.engine.model.DataEntity
import com.rosan.installer.domain.session.model.ProgressEntity
import kotlinx.coroutines.flow.MutableSharedFlow

interface NetworkResolver {
    suspend fun resolve(
        uri: Uri,
        cacheDirectory: String,
        progressFlow: MutableSharedFlow<ProgressEntity>
    ): List<DataEntity>
}