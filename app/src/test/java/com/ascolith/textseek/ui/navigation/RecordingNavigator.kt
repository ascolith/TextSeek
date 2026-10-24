package com.ascolith.textseek.ui.navigation

import androidx.navigation3.runtime.NavKey

internal class RecordingNavigator : EntryNavigator {
    val navigations = mutableListOf<Pair<NavKey, NavKey>>()
    val closes = mutableListOf<NavKey>()

    override fun navigate(from: NavKey, to: NavKey) {
        navigations += from to to
    }

    override fun close(route: NavKey) {
        closes += route
    }
}
