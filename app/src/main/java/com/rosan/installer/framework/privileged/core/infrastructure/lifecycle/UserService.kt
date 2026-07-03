package com.rosan.installer.framework.privileged.core.infrastructure.lifecycle

import com.rosan.installer.IPrivilegedService
import java.io.Closeable

interface UserService : Closeable {
    val privileged: IPrivilegedService
}