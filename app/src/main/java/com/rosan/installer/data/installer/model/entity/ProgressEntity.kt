package com.rosan.installer.data.installer.model.entity

sealed class ProgressEntity {
    data object Finish : ProgressEntity()

    data object Ready : ProgressEntity()

    data object Error : ProgressEntity()

    data object Resolving : ProgressEntity()

    data object ResolvedFailed : ProgressEntity()

    data object ResolveSuccess : ProgressEntity()

    data object Analysing : ProgressEntity()

    data object AnalysedFailed : ProgressEntity()

    data object AnalysedSuccess : ProgressEntity()

    data object Installing : ProgressEntity()

    data object InstallFailed : ProgressEntity()

    data object InstallSuccess : ProgressEntity()
}