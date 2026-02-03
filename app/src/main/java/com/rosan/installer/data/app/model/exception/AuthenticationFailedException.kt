package com.rosan.installer.data.app.model.exception

class AuthenticationFailedException  : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}