package com.rosan.installer.data.app.model.exception

class InstallFailedMediaUnavailableException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}