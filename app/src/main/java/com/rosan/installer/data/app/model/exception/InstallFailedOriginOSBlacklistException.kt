package com.rosan.installer.data.app.model.exception

class InstallFailedOriginOSBlacklistException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}