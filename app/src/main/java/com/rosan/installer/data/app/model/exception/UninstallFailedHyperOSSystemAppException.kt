package com.rosan.installer.data.app.model.exception

class UninstallFailedHyperOSSystemAppException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}