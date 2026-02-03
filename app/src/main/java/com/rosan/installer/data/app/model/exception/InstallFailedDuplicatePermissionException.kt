package com.rosan.installer.data.app.model.exception


class InstallFailedDuplicatePermissionException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}