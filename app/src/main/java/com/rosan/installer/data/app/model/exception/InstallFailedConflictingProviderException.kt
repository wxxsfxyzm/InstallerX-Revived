package com.rosan.installer.data.app.model.exception

class InstallFailedConflictingProviderException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}