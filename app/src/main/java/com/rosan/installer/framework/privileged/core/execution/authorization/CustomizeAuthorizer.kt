// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.execution.authorization

import com.rosan.installer.domain.privileged.exception.PrivilegedException
import com.rosan.installer.domain.privileged.model.PrivilegedErrorType

fun requireCustomizeAuthorizer(customizeAuthorizer: String): String {
    if (customizeAuthorizer.isBlank()) {
        throw PrivilegedException(
            errorType = PrivilegedErrorType.CUSTOM_AUTHORIZER_EMPTY,
            message = "Custom authorizer is empty."
        )
    }
    return customizeAuthorizer
}
