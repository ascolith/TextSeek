package com.ascolith.textseek.feature.debug.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.ConfigurableScaffold
import com.ascolith.textseek.core.model.ScaffoldConfig
import com.ascolith.textseek.resources.R
import kotlinx.serialization.Serializable

@Serializable
object DebugMenuScreenRoute : NavKey, ConfigurableScaffold {

    override fun getScaffoldConfig(): ScaffoldConfig =
        ScaffoldConfig(
            topBarTitleResId = R.string.debug_menu,
            showBottomBar = false,
            showBackButton = true,
        )
}
