package com.ascolith.textseek.feature.overview.ui.composable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.overview.ui.model.OverviewItemUiModel
import com.ascolith.textseek.feature.overview.ui.model.SearchGroupUiModel
import com.ascolith.textseek.feature.overview.ui.model.SearchSettingUiModel

internal val mockOverviewItems = mapOf(
    SearchGroupId(1) to OverviewItemUiModel(
        group = SearchGroupUiModel(
            id = SearchGroupId(1),
            name = "Favorite Foods",
            enabled = true,
            groupColorArgb = Color.Yellow.toArgb(),
        ),
        settings = mapOf(
            SearchSettingId(1) to SearchSettingUiModel(
                id = SearchSettingId(1),
                text = "Pizza",
                caseSensitive = false,
                colorArgb = Color.Yellow.toArgb(),
                enabled = true,
                searchGroupId = SearchGroupId(1),
            ),
            SearchSettingId(2) to SearchSettingUiModel(
                id = SearchSettingId(2),
                text = "Buffalo Wings",
                caseSensitive = true,
                colorArgb = Color.Red.toArgb(),
                enabled = false,
                searchGroupId = SearchGroupId(1),
            ),
            SearchSettingId(3) to SearchSettingUiModel(
                id = SearchSettingId(3),
                text = "Ribs",
                caseSensitive = false,
                colorArgb = Color.LightGray.toArgb(),
                enabled = true,
                searchGroupId = SearchGroupId(1),
            ),
        ),
    ),
    SearchGroupId(2) to OverviewItemUiModel(
        group = SearchGroupUiModel(
            id = SearchGroupId(2),
            name = "Allergens",
            enabled = false,
            groupColorArgb = Color.Blue.toArgb(),
        ),
        settings = mapOf(
            SearchSettingId(3) to SearchSettingUiModel(
                id = SearchSettingId(3),
                text = "Peanuts",
                caseSensitive = true,
                colorArgb = Color.Green.toArgb(),
                enabled = true,
                searchGroupId = SearchGroupId(2),
            ),
            SearchSettingId(4) to SearchSettingUiModel(
                id = SearchSettingId(4),
                text = "Shellfish",
                caseSensitive = false,
                colorArgb = Color.Gray.toArgb(),
                enabled = false,
                searchGroupId = SearchGroupId(2),
            ),
            SearchSettingId(5) to SearchSettingUiModel(
                id = SearchSettingId(5),
                text = "Milk",
                caseSensitive = false,
                colorArgb = Color.Blue.toArgb(),
                enabled = true,
                searchGroupId = SearchGroupId(2),
            ),
        ),
    ),
)
