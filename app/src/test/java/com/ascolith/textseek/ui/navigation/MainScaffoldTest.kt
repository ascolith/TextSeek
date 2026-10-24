package com.ascolith.textseek.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ascolith.textseek.core.model.ActionConfig
import com.ascolith.textseek.core.model.FabConfig
import com.ascolith.textseek.core.model.ScaffoldConfig
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.core.ui.TextSeekTheme
import com.ascolith.textseek.resources.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class MainScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val owner = Any()
    private val scaffoldState = TsScaffoldState()
    private var backClicks = 0
    private val selectedDestinations = mutableListOf<TsTopDestination>()

    private fun setContent(
        config: ScaffoldConfig,
        currentTopDestination: TsTopDestination? = TsTopDestination.Search,
    ) {
        scaffoldState.updateOwner(owner, config)
        composeTestRule.setContent {
            TextSeekTheme {
                MainScaffold(
                    scaffoldState = scaffoldState,
                    currentTopDestination = currentTopDestination,
                    onBack = { backClicks++ },
                    onTopDestinationSelected = { selectedDestinations += it },
                ) {
                    Text(CONTENT)
                }
            }
        }
    }

    @Test
    fun `the screen's content is shown`() {
        setContent(ScaffoldConfig())

        composeTestRule.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    @Test
    fun `the top bar shows the screen's title`() {
        setContent(ScaffoldConfig(topBarTitleResId = R.string.report_issue))

        composeTestRule.onNodeWithText(string(R.string.report_issue)).assertIsDisplayed()
    }

    @Test
    fun `a screen without a title has no top bar`() {
        setContent(ScaffoldConfig(topBarTitleResId = null, showBackButton = true))

        composeTestRule.onNodeWithContentDescription(string(R.string.back)).assertDoesNotExist()
    }

    @Test
    fun `the back button goes back`() {
        setContent(ScaffoldConfig(topBarTitleResId = R.string.report_issue, showBackButton = true))

        composeTestRule.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun `a screen can hide the back button`() {
        setContent(ScaffoldConfig(topBarTitleResId = R.string.report_issue, showBackButton = false))

        composeTestRule.onNodeWithContentDescription(string(R.string.back)).assertDoesNotExist()
    }

    @Test
    fun `top bar actions report their index when clicked`() {
        val clickedActions = mutableListOf<Int>()
        setContent(
            ScaffoldConfig(
                topBarTitleResId = R.string.report_issue,
                actions = listOf(
                    ActionConfig(iconResId = R.drawable.ic_add, labelResId = R.string.create_search_group),
                    ActionConfig(iconResId = R.drawable.ic_search, labelResId = R.string.search),
                ),
            )
        )
        scaffoldState.updateActionClickListener(owner) { clickedActions += it }

        composeTestRule.onNodeWithContentDescription(string(R.string.search)).performClick()
        composeTestRule.onNodeWithContentDescription(string(R.string.create_search_group)).performClick()

        assertEquals(listOf(1, 0), clickedActions)
    }

    @Test
    fun `a screen without a fab has none`() {
        setContent(ScaffoldConfig(fabConfig = null))

        composeTestRule.onNodeWithText(string(R.string.create_search_group), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `the fab reports clicks`() {
        var fabClicks = 0
        setContent(
            ScaffoldConfig(
                fabConfig = FabConfig(iconResId = R.drawable.ic_add, textResId = R.string.create_search_group),
            )
        )
        scaffoldState.updateFabClickListener(owner) { fabClicks++ }

        // The fab's label isn't merged into it, so it's found in the unmerged tree
        composeTestRule.onNodeWithText(string(R.string.create_search_group), useUnmergedTree = true)
            .performClick()

        assertEquals(1, fabClicks)
    }

    @Test
    fun `the bottom bar is shown on a top destination`() {
        setContent(ScaffoldConfig(showBottomBar = true), currentTopDestination = TsTopDestination.Search)

        TsTopDestination.entries.forEach { destination ->
            composeTestRule.onNodeWithText(string(destination.label)).assertIsDisplayed()
        }
    }

    @Test
    fun `the bottom bar is hidden off the top destinations`() {
        setContent(ScaffoldConfig(showBottomBar = true), currentTopDestination = null)

        composeTestRule.onNodeWithText(string(TsTopDestination.Overview.label)).assertDoesNotExist()
    }

    @Test
    fun `a top destination can hide the bottom bar`() {
        setContent(ScaffoldConfig(showBottomBar = false), currentTopDestination = TsTopDestination.Search)

        composeTestRule.onNodeWithText(string(TsTopDestination.Overview.label)).assertDoesNotExist()
    }

    @Test
    fun `selecting another top destination reports it`() {
        setContent(ScaffoldConfig(showBottomBar = true), currentTopDestination = TsTopDestination.Search)

        composeTestRule.onNodeWithText(string(TsTopDestination.Overview.label)).performClick()

        assertEquals(listOf(TsTopDestination.Overview), selectedDestinations)
    }

    @Test
    fun `reselecting the current top destination is ignored`() {
        setContent(ScaffoldConfig(showBottomBar = true), currentTopDestination = TsTopDestination.Search)

        composeTestRule.onNodeWithText(string(TsTopDestination.Search.label)).performClick()

        assertEquals(emptyList<TsTopDestination>(), selectedDestinations)
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun `a short window shows a rail on a top destination`() {
        setContent(ScaffoldConfig(showBottomBar = true), currentTopDestination = TsTopDestination.Search)

        val bounds = TsTopDestination.entries.map { destination ->
            composeTestRule.onNodeWithText(string(destination.label))
                .assertIsDisplayed()
                .getUnclippedBoundsInRoot()
        }
        // Stacked in a column rather than side by side in a bar
        assertEquals(1, bounds.map { (it.left + it.right) / 2 }.distinct().size)
        assertEquals(bounds.sortedBy { it.top }, bounds)
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun `a top destination can hide the rail`() {
        setContent(ScaffoldConfig(showBottomBar = false), currentTopDestination = TsTopDestination.Search)

        composeTestRule.onNodeWithText(string(TsTopDestination.Overview.label)).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun `selecting another top destination on the rail reports it`() {
        setContent(ScaffoldConfig(showBottomBar = true), currentTopDestination = TsTopDestination.Search)

        composeTestRule.onNodeWithText(string(TsTopDestination.Settings.label)).performClick()

        assertEquals(listOf(TsTopDestination.Settings), selectedDestinations)
    }

    private companion object {
        const val CONTENT = "content"
        const val LANDSCAPE = "w891dp-h411dp-land"
    }
}
