package com.rosan.installer.data.app.model.exception

class InstallFailedInvalidURIException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}