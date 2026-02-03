package com.rosan.installer.ui.util

import com.rosan.installer.data.settings.model.room.entity.ConfigEntity

/**
 * Returns true if the Dhizuku authorizer is active, which disables certain features.
 */
fun isDhizukuActive(
    stateAuthorizer: ConfigEntity.Authorizer,
    globalAuthorizer: ConfigEntity.Authorizer
) = when (stateAuthorizer) {
    ConfigEntity.Authorizer.Dhizuku -> true
    ConfigEntity.Authorizer.Global -> globalAuthorizer == ConfigEntity.Authorizer.Dhizuku
    else -> false
}

/**
 * Returns true if the None authorizer is active.
 */
fun isNoneActive(
    stateAuthorizer: ConfigEntity.Authorizer,
    globalAuthorizer: ConfigEntity.Authorizer
) = when (stateAuthorizer) {
    ConfigEntity.Authorizer.None -> true
    ConfigEntity.Authorizer.Global -> globalAuthorizer == ConfigEntity.Authorizer.None
    else -> false
}
