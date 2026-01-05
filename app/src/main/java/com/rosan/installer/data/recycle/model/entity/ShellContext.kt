package com.rosan.installer.data.recycle.model.entity

import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.os.Build

class ShellContext(base: Context) : ContextWrapper(base) {
    override fun getOpPackageName(): String {
        return "com.android.shell"
    }

    override fun getAttributionSource(): AttributionSource {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 伪造 AttributionSource，UID 为 Shell (2000)，包名为 com.android.shell
            val shellUid = 2000 // 或者使用 Shizuku.getUid() 如果引入了库
            val builder = AttributionSource.Builder(shellUid)
                .setPackageName("com.android.shell")

            // Android 14 (UPSIDE_DOWN_CAKE) 需要特殊处理 PID
            if (Build.VERSION.SDK_INT >= 34) {
                builder.setPid(android.os.Process.INVALID_PID)
            }

            return builder.build()
        }
        return super.getAttributionSource()
    }
}