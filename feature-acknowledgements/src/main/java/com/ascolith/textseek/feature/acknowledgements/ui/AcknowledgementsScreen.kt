package com.ascolith.textseek.feature.acknowledgements.ui

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

/**
 * Lists the app's open source dependencies and their licenses.
 *
 * [librariesResId] is the metadata the AboutLibraries Gradle plugin generates in the app
 * module, which is the only module that sees the full dependency graph.
 */
@Composable
fun AcknowledgementsScreen(
    @RawRes librariesResId: Int,
    modifier: Modifier = Modifier,
) {
    val libraries by produceLibraries(librariesResId)

    LibrariesContainer(
        libraries = libraries,
        modifier = modifier.fillMaxSize(),
    )
}
