// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.settings.config.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R

@Composable
internal fun EditViewState.dhizukuAwareDescription(defaultDescription: String): String =
    if (isDhizukuAuthorizerActive) {
        stringResource(R.string.dhizuku_unsupported_desc)
    } else {
        defaultDescription
    }
