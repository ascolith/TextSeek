package com.ascolith.textseek.feature.overview.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.ascolith.textseek.core.composable.TsSemantics
import com.ascolith.textseek.core.ui.TextSeekTheme
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.overview.ui.model.OverviewItemUiModel
import com.ascolith.textseek.feature.overview.ui.model.SearchGroupUiModel
import com.ascolith.textseek.feature.overview.ui.model.SearchSettingUiModel
import com.ascolith.textseek.resources.R
import org.junit.Rule
import org.junit.Test

class OverviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `no groups shows the empty state`() {
        composeTestRule.setContent {
            TextSeekTheme {
                OverviewScreen(
                    overviewItems = emptyList(),
                    onIntent = {}
                )
            }
        }

        val emptyText = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.add_group_to_get_started)
        composeTestRule
            .onNodeWithText(emptyText)
            .assertIsDisplayed()
    }

    @Test
    fun `setting without custom color has default group color border`() {
        val settingName = "Setting"
        val groupColor = Color.Red

        composeTestRule.setContent {
            TextSeekTheme {
                OverviewScreen(
                    overviewItems = createOverviewItems(
                        groupColorArgb = groupColor.toArgb(),
                        settingColorArgb = null,
                    ),
                    onIntent = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(settingName)
            .assert(
                SemanticsMatcher.expectValue(TsSemantics.borderColorKey, groupColor)
            )
    }

    @Test
    fun `setting with custom color has that color border`() {
        val settingName = "Setting"
        val groupColor = Color.Red
        val settingColor = Color.Yellow

        composeTestRule.setContent {
            TextSeekTheme {
                OverviewScreen(
                    overviewItems = createOverviewItems(
                        groupColorArgb = groupColor.toArgb(),
                        settingColorArgb = settingColor.toArgb(),
                    ),
                    onIntent = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(settingName)
            .assert(
                SemanticsMatcher.expectValue(TsSemantics.borderColorKey, settingColor)
            )
    }

    @Test
    fun `disabled setting with custom color has no border color`() {
        val settingName = "Setting"
        val groupColor = Color.Red
        val settingColor = Color.Yellow

        composeTestRule.setContent {
            TextSeekTheme {
                OverviewScreen(
                    overviewItems = createOverviewItems(
                        groupColorArgb = groupColor.toArgb(),
                        settingColorArgb = settingColor.toArgb(),
                        enabled = false,
                    ),
                    onIntent = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(settingName)
            .assert(
                SemanticsMatcher.expectValue(TsSemantics.borderColorKey, Color.Transparent)
            )
    }

    @Test
    fun `disabled setting with no custom color has no border color`() {
        val settingName = "Setting"
        val groupColor = Color.Red

        composeTestRule.setContent {
            TextSeekTheme {
                OverviewScreen(
                    overviewItems = createOverviewItems(
                        groupColorArgb = groupColor.toArgb(),
                        settingColorArgb = null,
                        enabled = false,
                    ),
                    onIntent = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(settingName)
            .assert(
                SemanticsMatcher.expectValue(TsSemantics.borderColorKey, Color.Transparent)
            )
    }

    private fun createOverviewItems(
        groupColorArgb: Int,
        settingColorArgb: Int?,
        groupName: String = "Test Group",
        settingText: String = "Setting",
        enabled: Boolean = true,
    ): List<OverviewItemUiModel> {
        return listOf(
            createGroup(
                idNumber = 1,
                name = groupName,
                groupColorArgb = groupColorArgb,
            ) { searchGroupId ->
                listOf(
                    createSetting(
                        idNumber = 0,
                        group = searchGroupId,
                        text = settingText,
                        enabled = enabled,
                        colorArgb = settingColorArgb,
                    )
                ).associateBy { it.id }
            }
        )
    }

    private fun createGroup(
        idNumber: Long,
        name: String,
        enabled: Boolean = true,
        groupColorArgb: Int? = null,
        buildSettings: (SearchGroupId) -> Map<SearchSettingId, SearchSettingUiModel>,
    ): OverviewItemUiModel {
        val id = SearchGroupId(idNumber)
        return OverviewItemUiModel(
            group = SearchGroupUiModel(
                id = id,
                name = name,
                enabled = enabled,
                groupColorArgb = groupColorArgb,
            ),
            settings = buildSettings(id),
        )
    }

    private fun createSetting(
        idNumber: Long,
        group: SearchGroupId,
        text: String,
        enabled: Boolean = true,
        caseSensitive: Boolean = false,
        colorArgb: Int? = null,
    ): SearchSettingUiModel {
        return SearchSettingUiModel(
            id = SearchSettingId(idNumber),
            text = text,
            caseSensitive = caseSensitive,
            colorArgb = colorArgb,
            enabled = enabled,
            searchGroupId = group,
        )
    }
}
