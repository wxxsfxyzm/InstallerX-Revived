package com.rosan.installer.data.recycle.model.exception

class ShizukuNotWorkException : RuntimeException {
    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(cause: Throwable?) : super(cause)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
