package com.rosan.installer.data.app.model.exception

class InstallFailedVerificationTimeoutException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}