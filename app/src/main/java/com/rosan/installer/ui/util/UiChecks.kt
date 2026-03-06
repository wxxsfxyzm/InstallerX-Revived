package com.rosan.installer.ui.util

import com.rosan.installer.domain.settings.model.Authorizer

/**
 * Returns true if the Dhizuku authorizer is active, which disables certain features.
 */
fun isDhizukuActive(
    stateAuthorizer: Authorizer,
    globalAuthorizer: Authorizer
) = when (stateAuthorizer) {
    Authorizer.Dhizuku -> true
    Authorizer.Global -> globalAuthorizer == Authorizer.Dhizuku
    else -> false
}

/**
 * Returns true if the None authorizer is active.
 */
fun isNoneActive(
    stateAuthorizer: Authorizer,
    globalAuthorizer: Authorizer
) = when (stateAuthorizer) {
    Authorizer.None -> true
    Authorizer.Global -> globalAuthorizer == Authorizer.None
    else -> false
}
