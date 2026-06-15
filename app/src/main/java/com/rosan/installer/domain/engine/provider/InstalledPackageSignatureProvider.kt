// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.provider

interface InstalledPackageSignatureProvider {
    fun hasSigningCertificate(packageName: String, certificateSha256: String): Boolean
}
