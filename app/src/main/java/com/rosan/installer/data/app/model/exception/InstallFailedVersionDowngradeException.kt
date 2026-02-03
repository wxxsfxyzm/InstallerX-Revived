package com.rosan.installer.data.app.model.exception

class InstallFailedVersionDowngradeException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}