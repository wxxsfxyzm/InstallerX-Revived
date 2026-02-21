package com.rosan.installer

import android.system.ErrnoException

object Natives {
    init {
        System.loadLibrary("installerx")
    }

    /**
     * Delete a file directly use native API
     * @param filePath The path to the file need to be deleted
     * @exception ErrnoException any failed errno from kernel
     */
    @Throws(ErrnoException::class)
    external fun deleteFile(filePath: String)
}