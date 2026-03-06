package com.rosan.installer.domain.session.model

import com.rosan.installer.domain.engine.model.AppEntity

data class SelectInstallEntity(
    val app: AppEntity,
    val selected: Boolean
)