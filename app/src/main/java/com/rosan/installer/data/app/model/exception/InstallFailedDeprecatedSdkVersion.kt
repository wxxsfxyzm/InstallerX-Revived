package com.rosan.installer.data.app.model.exception

class InstallFailedDeprecatedSdkVersion : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}