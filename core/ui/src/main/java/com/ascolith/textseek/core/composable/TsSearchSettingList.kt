package com.ascolith.textseek.core.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ascolith.textseek.domain.model.SearchSettingId
import androidx.compose.foundation.lazy.itemsIndexed

@Composable
fun TsSearchSettingList(
    settings: List<TsSearchSettingItemData>,
    defaultColor: Int?,
    onItemClick: (SearchSettingId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(
            settings,
            key = { _, item -> item.searchSettingId.value }
        ) { index, setting ->
            val isSingleItem = settings.count() == 1
            val topCornerRadius = if (isSingleItem || index == 0) 12.dp else 4.dp
            val bottomCornerRadius = if (isSingleItem || index == settings.size - 1) 12.dp else 4.dp

            TsSearchSettingCard(
                modifier = Modifier.padding(vertical = 2.dp),
                searchSettingId = setting.searchSettingId,
                text = setting.text,
                enabled = setting.enabled,
                indicatorColor = requireColor(setting.colorArgb ?: defaultColor),
                onClick = { onItemClick(it) },
                topCornerRadius = topCornerRadius,
                bottomCornerRadius = bottomCornerRadius,
            )
        }
    }
}

private fun requireColor(colorArgb: Int?): Color {
    requireNotNull(colorArgb)
    return Color(colorArgb)
}
