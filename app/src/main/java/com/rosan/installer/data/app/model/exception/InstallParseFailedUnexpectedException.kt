package com.rosan.installer.data.app.model.exception

class InstallParseFailedUnexpectedException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}