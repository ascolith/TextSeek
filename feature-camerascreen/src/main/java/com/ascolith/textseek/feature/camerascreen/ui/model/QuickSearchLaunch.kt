package com.ascolith.textseek.feature.camerascreen.ui.model

import androidx.annotation.ColorInt

/**
 * Parameters for launching the camera screen directly into Quick Search,
 * e.g. from the launcher shortcut or an assistant app function.
 *
 * A null [query] opens the empty Quick Search dialog for the user to type
 * into; a non-blank [query] starts an active search immediately.
 *
 * [launchId] distinguishes launch deliveries. The screen records the id it last
 * handled and compares by identity, so a redelivery of the same launch is ignored
 * while a new delivery (even with identical parameters) triggers again. Ids are
 * therefore only required to be distinct per delivery, not ordered: the caller must
 * carry the id with the launch itself so that a redelivery repeats it, rather than
 * generating one at delivery time.
 */
data class QuickSearchLaunch(
    val launchId: Long,
    val query: String? = null,
    val caseSensitive: Boolean = false,
    @param:ColorInt val colorArgb: Int? = null,
)
