// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.device.provider.AppInfoProvider

class GetAppLabelUseCase(private val appInfoProvider: AppInfoProvider) {
    /**
     * Resolves and formats the application label.
     * Formats the result as "AppLabel (packageName)" for better context.
     * Fallbacks to the raw package name if the app is not found.
     *
     * @param packageName The package name to resolve.
     * @return The formatted label string.
     */
    suspend operator fun invoke(packageName: String): String {
        val label = appInfoProvider.getAppLabel(packageName)
        return if (label != null) {
            "$label ($packageName)"
        } else {
            packageName
        }
    }
}
