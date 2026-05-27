package com.rosan.installer.framework.privileged.lifecycle

import com.rosan.installer.IPrivilegedService
import java.io.Closeable

interface UserService : Closeable {
    val privileged: IPrivilegedService
}