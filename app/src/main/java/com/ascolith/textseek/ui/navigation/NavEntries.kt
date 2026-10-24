package com.ascolith.textseek.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

// A destination on the bottom bar, which slides in the bar's order
internal inline fun <reified K : NavKey> EntryProviderScope<NavKey>.topDestinationEntry(
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(
        metadata = { route ->
            barPosition(checkNotNull(route.toTsTopDestinationOrNull()) { "$route is not a top destination" })
        },
        content = content,
    )
}

// An editor, which rises over the screen it was opened from
internal inline fun <reified K : NavKey> EntryProviderScope<NavKey>.editorEntry(
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(metadata = riseAndFall) { route ->
        EditorSurface { content(route) }
    }
}

// Editors rise over another screen, so they need a background of their own
@Composable
internal fun EditorSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        content = content,
    )
}
