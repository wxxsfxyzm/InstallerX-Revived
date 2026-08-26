// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.error

import androidx.annotation.StringRes
import com.rosan.installer.R

enum class AnalyseErrorType(@param:StringRes val stringResId: Int) {
    CORRUPTED_ARCHIVE(R.string.installer_analyse_failed_corrupted_archive),
    STREAMING_SOURCE_UNSUPPORTED(R.string.exception_analyse_failed_streaming_source_unsupported),
    SOURCE_MATERIALIZATION_FAILED(R.string.exception_analyse_failed_source_materialization),
    ALL_FILES_UNSUPPORTED(R.string.exception_analyse_failed_all_files_unsupported),
}
