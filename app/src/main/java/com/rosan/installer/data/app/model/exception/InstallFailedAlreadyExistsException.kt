package com.rosan.installer.data.app.model.exception

class InstallFailedAlreadyExistsException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}