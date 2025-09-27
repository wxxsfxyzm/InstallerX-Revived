package com.rosan.installer.data.app.model.exception

class InstallFailedMissingSplitException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}