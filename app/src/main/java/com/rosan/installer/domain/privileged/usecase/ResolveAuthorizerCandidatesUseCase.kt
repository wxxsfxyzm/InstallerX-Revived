// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.domain.privileged.usecase

import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.SmartAuthorizerPreferences
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.StringSetting
import kotlinx.coroutines.flow.first

/**
 * Builds the ordered list of authorizers to attempt for a privileged operation: the preferred one
 * first, then the enabled entries of the smart authorizer fallback list.
 */
class ResolveAuthorizerCandidatesUseCase(
    private val appSettingsRepo: AppSettingsRepository,
    private val capabilityProvider: DeviceCapabilityProvider,
) {
    suspend operator fun invoke(preferred: Authorizer, customizeAuthorizer: String): List<Authorizer> {
        val fallbackAuthorizers = SmartAuthorizerPreferences.decode(
            value = appSettingsRepo.getString(StringSetting.SmartAuthorizerCandidates).first(),
            isSessionInstallSupported = capabilityProvider.isSessionInstallSupported,
        ).filter { it.enabled }.map { it.authorizer }

        return buildList {
            if (preferred != Authorizer.Global) add(preferred)
            addAll(fallbackAuthorizers)
        }.filter { authorizer ->
            // Global can no longer reach this point, but the guard stays so a future addition to the
            // supported authorizer set cannot leak it through: it has no backend of its own.
            authorizer != Authorizer.Global &&
                (authorizer != Authorizer.Customize || customizeAuthorizer.isNotBlank())
        }.distinct()
    }
}
