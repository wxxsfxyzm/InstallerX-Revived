package com.rosan.installer.data.app.model.exception

class InstallFailedMissingFeatureException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}