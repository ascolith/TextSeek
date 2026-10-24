package com.ascolith.textseek.core.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.resources.R

data class TsSearchGroupItemData(
    val searchGroupId: SearchGroupId,
    val name: String,
    val enabled: Boolean,
    val groupColorArgb: Int?,
    val settings: Map<SearchSettingId, TsSearchSettingItemData>,
)

data class TsSearchSettingItemData(
    val searchSettingId: SearchSettingId,
    val text: String,
    val enabled: Boolean,
    val colorArgb: Int?,
)

@Composable
fun TsSearchGroupItem(
    data: TsSearchGroupItemData,
    onGroupEnabledToggle: (SearchGroupId) -> Unit,
    onSearchSettingAddClick: (SearchGroupId) -> Unit,
    onSearchSettingEditClick: (SearchSettingId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = data.name,
                    style = if (!data.enabled) {
                        LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                    } else {
                        LocalTextStyle.current
                    }
                )
            }


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val switchDescription = stringResource(R.string.search_group_enabled)
                Switch(
                    modifier = Modifier.semantics { contentDescription = switchDescription },
                    checked = data.enabled,
                    onCheckedChange = { onGroupEnabledToggle(data.searchGroupId) },
                )

                IconButton(
                    onClick = { onSearchSettingAddClick(data.searchGroupId) }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_search_term),
                    )
                }
            }
        }

        val settingsList = data.settings.values.toList()
        if (settingsList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        TsSearchSettingList(
            modifier = Modifier.heightIn(max = 200.dp),
            settings = settingsList,
            defaultColor = data.groupColorArgb,
            onItemClick = onSearchSettingEditClick
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewTsGroupItem() {
    TsPreview {
        TsSearchGroupItem(
            data = TsSearchGroupItemData(
                searchGroupId = SearchGroupId(0),
                name = "Fruits",
                enabled = true,
                groupColorArgb = Color.Green.toArgb(),
                settings = listOf(
                    TsSearchSettingItemData(
                        searchSettingId = SearchSettingId(0),
                        text = "Banana",
                        enabled = true,
                        colorArgb = Color.Yellow.toArgb(),
                    ),
                    TsSearchSettingItemData(
                        searchSettingId = SearchSettingId(1),
                        text = "Apple",
                        enabled = false,
                        colorArgb = Color.Red.toArgb(),
                    ),
                    TsSearchSettingItemData(
                        searchSettingId = SearchSettingId(2),
                        text = "Grape",
                        enabled = true,
                        colorArgb = Color.Blue.toArgb(),
                    ),
                ).associateBy { it.searchSettingId }
            ),
            onGroupEnabledToggle = {},
            onSearchSettingAddClick = {},
            onSearchSettingEditClick = {},
        )
    }
}
