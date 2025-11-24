package com.rosan.installer.data.app.model.entity

/**
 * 定义安装会话的交互模式。
 * 决定了 UI 如何展示（列表 vs 详情）以及安装流程如何调度。
 */
enum class SessionMode {
    /**
     * 单应用模式。
     * UI 展示：详情页（Banner、介绍、截图等）。
     * 逻辑：通常只涉及一个主包，或者一个 Split 套件。
     */
    Single,

    /**
     * 批量/多应用模式。
     * UI 展示：应用列表（List Item）。
     * 逻辑：涉及多个独立的 AppEntity，需要队列安装。
     */
    Batch
}