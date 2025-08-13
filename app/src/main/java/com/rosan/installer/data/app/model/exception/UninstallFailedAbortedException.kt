package com.rosan.installer.data.app.model.exception

class UninstallFailedAbortedException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}