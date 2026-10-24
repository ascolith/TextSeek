package com.ascolith.textseek.domain.model

data class SearchGroup(
    val id: SearchGroupId,
    val name: String,
    val enabled: Boolean,
    val colorArgb: Int,
) {
    companion object {
        /**
         * Initial color for new search groups, and the fallback color for a
         * quick search that has no color of its own.
         */
        const val DEFAULT_COLOR_ARGB = 0xFFFFFF00.toInt()
    }
}
