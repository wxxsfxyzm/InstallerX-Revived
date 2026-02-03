package com.rosan.installer.data.app.model.exception

class InstallFailedNoSharedUserException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}