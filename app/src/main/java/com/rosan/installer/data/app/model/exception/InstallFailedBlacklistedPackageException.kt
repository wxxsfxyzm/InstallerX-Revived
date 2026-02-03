package com.rosan.installer.data.app.model.exception

class InstallFailedBlacklistedPackageException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}