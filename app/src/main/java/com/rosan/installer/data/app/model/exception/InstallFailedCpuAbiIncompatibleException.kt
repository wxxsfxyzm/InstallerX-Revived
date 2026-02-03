package com.rosan.installer.data.app.model.exception

class InstallFailedCpuAbiIncompatibleException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}