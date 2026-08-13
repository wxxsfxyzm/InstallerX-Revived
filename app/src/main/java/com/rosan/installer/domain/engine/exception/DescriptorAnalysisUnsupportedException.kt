// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.exception

import com.rosan.installer.domain.engine.model.error.AnalyseErrorType
import com.rosan.installer.domain.engine.model.source.DataEntity

class DescriptorAnalysisUnsupportedException(
    val source: DataEntity.FileDescriptorEntity,
    cause: Throwable
) : AnalyseException(
    errorType = AnalyseErrorType.STREAMING_SOURCE_UNSUPPORTED,
    message = "The descriptor-backed APK source cannot be analysed directly",
    cause = cause
)
