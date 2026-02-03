package com.rosan.installer.data.app.model.exception

class InstallFailedDexOptException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}