package com.rosan.installer.data.installer.model.exception

class ResolvedFailedNoInternetAccessException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)
}