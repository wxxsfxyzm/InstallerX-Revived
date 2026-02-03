package com.rosan.installer.data.app.model.exception

class InstallFailedTestOnlyException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}