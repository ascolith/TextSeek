package com.ascolith.textseek.feature.searchgroup.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.ConfigurableScaffold
import com.ascolith.textseek.core.model.ScaffoldConfig
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.resources.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchGroupScreenRoute(
    @SerialName("id") val id: SearchGroupId? = null,
) : NavKey, ConfigurableScaffold {

    override fun getScaffoldConfig(): ScaffoldConfig =
        ScaffoldConfig(
            topBarTitleResId = if (this.id == null) R.string.create_group else R.string.edit_group,
            showBottomBar = false,
            showBackButton = true,
        )
}
