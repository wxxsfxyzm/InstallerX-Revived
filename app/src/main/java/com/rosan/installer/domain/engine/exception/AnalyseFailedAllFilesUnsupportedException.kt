package com.rosan.installer.domain.engine.exception

import com.rosan.installer.R

class AnalyseFailedAllFilesUnsupportedException : InstallerException {
    constructor() : super()

    constructor(message: String?) : super(message)

    override fun getStringResId(): Int {
        return R.string.exception_analyse_failed_all_files_unsupported
    }
}