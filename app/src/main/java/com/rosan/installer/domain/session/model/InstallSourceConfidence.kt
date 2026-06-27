// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.session.model

enum class InstallSourceConfidence {
    EXACT_CALLER,
    LAUNCHED_FROM_UID,
    TRUSTED_PROXY_ORIGINATING_UID,
    PROVIDER_OWNER,
    REFERRER_HEURISTIC,
    UNKNOWN;

    fun isTrustedForPlatformPolicy(): Boolean =
        this == EXACT_CALLER ||
                this == LAUNCHED_FROM_UID ||
                this == TRUSTED_PROXY_ORIGINATING_UID
}