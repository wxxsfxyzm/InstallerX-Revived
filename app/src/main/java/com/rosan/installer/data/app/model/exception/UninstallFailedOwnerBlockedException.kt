package com.rosan.installer.data.app.model.exception

class UninstallFailedOwnerBlockedException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}