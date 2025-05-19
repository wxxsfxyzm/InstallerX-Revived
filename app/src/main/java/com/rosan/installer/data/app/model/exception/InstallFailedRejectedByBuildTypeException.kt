package com.rosan.installer.data.app.model.exception

class InstallFailedRejectedByBuildTypeException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}