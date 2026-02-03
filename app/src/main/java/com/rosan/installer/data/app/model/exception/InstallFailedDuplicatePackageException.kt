package com.rosan.installer.data.app.model.exception

class InstallFailedDuplicatePackageException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}