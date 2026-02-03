package com.rosan.installer.data.app.model.enums

/**
 * Defines the interaction mode for an installation session.
 * Determines how the UI is presented (list vs. details) and how the installation flow is scheduled.
 */
enum class SessionMode {
    /**
     * Single app mode.
     * UI Display: Details page (Banner, description, screenshots, etc.).
     * Logic: Usually involves a single main package, or a Split suite.
     */
    Single,

    /**
     * Batch/Multi-app mode.
     * UI Display: App list (List Item).
     * Logic: Involves multiple independent AppEntities, requiring queued installation.
     */
    Batch
}