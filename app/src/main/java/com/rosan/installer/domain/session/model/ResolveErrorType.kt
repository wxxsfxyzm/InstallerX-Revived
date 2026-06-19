// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

import androidx.annotation.StringRes
import com.rosan.installer.R

enum class ResolveErrorType(@param:StringRes val stringResId: Int) {
    GENERIC_FAILED(R.string.exception_resolve_failed),
    NO_INTERNET_ACCESS(R.string.exception_resolve_failed_no_internet_access),
    INITIATOR_NOT_VISIBLE(R.string.exception_initiator_not_visible),
    LINK_NOT_VALID(R.string.exception_resolve_failed_link_not_valid),

    HTTP_RESTRICTED_FOR_LOCALHOST(R.string.exception_http_restricted_for_localhost),
    HTTP_NOT_ALLOWED(R.string.exception_http_not_allowed)
}
