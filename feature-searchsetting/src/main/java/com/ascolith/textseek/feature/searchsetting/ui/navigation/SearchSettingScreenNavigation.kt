package com.ascolith.textseek.feature.searchsetting.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.ConfigurableScaffold
import com.ascolith.textseek.core.model.ScaffoldConfig
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.resources.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchSettingScreenRoute(
    @SerialName("id") val id: SearchSettingId? = null,
    @SerialName("groupIdDefault") val groupIdDefault: SearchGroupId? = null,
) : NavKey, ConfigurableScaffold {

    override fun getScaffoldConfig(): ScaffoldConfig =
        ScaffoldConfig(
            topBarTitleResId = if (this.id == null) R.string.create_search else R.string.edit_search,
            showBottomBar = false,
            showBackButton = true,
        )
}
