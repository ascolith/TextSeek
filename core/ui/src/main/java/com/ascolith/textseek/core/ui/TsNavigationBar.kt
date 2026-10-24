package com.ascolith.textseek.core.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.ascolith.textseek.resources.R

enum class TsTopDestination(
    val icon: Int,
    val label: Int,
) {
    Search(
        icon = R.drawable.ic_search,
        label = R.string.search
    ),
    Overview(
        icon = R.drawable.ic_group,
        label = R.string.manage
    ),
    Settings(
        icon = R.drawable.ic_settings,
        label = R.string.settings
    ),
}

@Composable
fun TsNavigationBar(
    currentDestination: TsTopDestination?,
    onNavigate: (TsTopDestination) -> Unit,
) {
    NavigationBar {
        TsTopDestination.entries.forEach { destination ->
            NavigationBarItem(
                icon = { DestinationIcon(destination) },
                label = { Text(stringResource(destination.label)) },
                selected = currentDestination == destination,
                onClick = { onNavigate(destination) }
            )
        }
    }
}

/** The insets [TsNavigationRail] pads itself by, including the cutout a landscape phone puts beside it. */
val tsNavigationRailInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)

/** [TsNavigationBar]'s counterpart for short windows, e.g. landscape phones. */
@Composable
fun TsNavigationRail(
    currentDestination: TsTopDestination?,
    onNavigate: (TsTopDestination) -> Unit,
) {
    NavigationRail(windowInsets = tsNavigationRailInsets) {
        Spacer(Modifier.weight(1f))
        TsTopDestination.entries.forEach { destination ->
            NavigationRailItem(
                icon = { DestinationIcon(destination) },
                label = { Text(stringResource(destination.label)) },
                selected = currentDestination == destination,
                onClick = { onNavigate(destination) }
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun DestinationIcon(destination: TsTopDestination) {
    Icon(
        imageVector = ImageVector.vectorResource(destination.icon),
        contentDescription = stringResource(destination.label)
    )
}
