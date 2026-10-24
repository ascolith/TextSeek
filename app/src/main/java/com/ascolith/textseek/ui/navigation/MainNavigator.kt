package com.ascolith.textseek.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.ascolith.textseek.core.ui.TsTopDestination

/** The back stack operations a screen's entry may perform. */
internal interface EntryNavigator {
    fun navigate(from: NavKey, to: NavKey)

    fun close(route: NavKey)
}

@Stable
internal class MainNavigator(private val backStack: NavBackStack<NavKey>) : EntryNavigator {

    val routes: List<NavKey> get() = backStack

    val currentRoute: NavKey get() = backStack.last()

    override fun navigate(from: NavKey, to: NavKey) {
        // Only the screen on top may open another
        if (backStack.lastOrNull() == from) open(to)
    }

    override fun close(route: NavKey) {
        // Only the screen on top may close itself
        if (backStack.lastOrNull() == route) goBack()
    }

    fun open(route: NavKey) {
        backStack.add(route)
    }

    fun goBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun navigateToTopDestination(destination: TsTopDestination) {
        if (destination == startDestination) {
            // Pop back to the start destination
            while (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        } else {
            // Keep the start destination at the bottom, replace everything else
            open(destination.toNavKey())
            while (backStack.size > 2) {
                backStack.removeAt(backStack.size - 2)
            }
        }
    }
}

@Composable
internal fun rememberMainNavigator(): MainNavigator {
    val backStack = rememberNavBackStack(startDestination.toNavKey())
    return remember(backStack) { MainNavigator(backStack) }
}
