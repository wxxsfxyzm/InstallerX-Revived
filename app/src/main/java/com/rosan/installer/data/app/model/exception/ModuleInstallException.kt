package com.rosan.installer.data.app.model.exception

/**
 * Custom exception for module installation failures.
 *
 * @param message A descriptive error message.
 * @param cause The underlying cause of the failure.
 */
class ModuleInstallException(message: String, cause: Throwable? = null) : Exception(message, cause)