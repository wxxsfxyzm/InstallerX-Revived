package com.rosan.installer.data.app.model.exception

class InstallFailedAbortedException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}