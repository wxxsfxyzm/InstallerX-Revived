package com.rosan.installer.data.app.model.exception

class InstallFailedNewerSDKException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}