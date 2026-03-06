package com.rosan.installer.data.privileged.repository.recyclable

import com.rosan.installer.IPrivilegedService
import java.io.Closeable

interface UserService : Closeable {
    val privileged: IPrivilegedService
}