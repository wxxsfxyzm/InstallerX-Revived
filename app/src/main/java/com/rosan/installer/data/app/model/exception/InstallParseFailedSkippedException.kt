package com.rosan.installer.data.app.model.exception

class InstallParseFailedSkippedException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}