package com.ascolith.textseek.feature.settings.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.ConfigurableScaffold
import com.ascolith.textseek.core.model.ScaffoldConfig
import com.ascolith.textseek.resources.R
import kotlinx.serialization.Serializable

@Serializable
object SettingsScreenRoute : NavKey, ConfigurableScaffold {

    override fun getScaffoldConfig(): ScaffoldConfig =
        ScaffoldConfig(
            topBarTitleResId = R.string.settings,
            showBottomBar = true,
        )
}
