package com.ascolith.textseek.feature.overview.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.ConfigurableScaffold
import com.ascolith.textseek.core.model.FabConfig
import com.ascolith.textseek.core.model.ScaffoldConfig
import com.ascolith.textseek.resources.R
import kotlinx.serialization.Serializable

@Serializable
data object OverviewScreenRoute : NavKey, ConfigurableScaffold {

    override fun getScaffoldConfig(): ScaffoldConfig =
        ScaffoldConfig(
            topBarTitleResId = R.string.manage,
            fabConfig = FabConfig(
                iconResId = R.drawable.ic_add,
                textResId = R.string.create_search_group,
            ),
            showBottomBar = true,
        )
}
