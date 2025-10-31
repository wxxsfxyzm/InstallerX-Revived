package com.rosan.installer.data.app.model.exception

class InstallParseFailedNoCertificatesException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}