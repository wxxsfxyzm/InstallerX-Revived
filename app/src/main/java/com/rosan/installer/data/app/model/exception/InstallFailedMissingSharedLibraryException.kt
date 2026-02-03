package com.rosan.installer.data.app.model.exception

class InstallFailedMissingSharedLibraryException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}