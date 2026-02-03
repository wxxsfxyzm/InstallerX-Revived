package com.rosan.installer.data.app.model.exception

class InstallFailedUidChangedException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}