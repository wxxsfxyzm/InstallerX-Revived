package com.rosan.installer.data.app.model.exception

class UninstallFailedUserRestrictedException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}