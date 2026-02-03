package com.rosan.installer.data.installer.model.exception

import android.net.Uri

data class ResolveException(
    val action: String?,
    val uris: List<Uri>,
) : Exception(
    "action: $action, uri: $uris"
)