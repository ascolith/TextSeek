package com.ascolith.textseek.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.feature.camerascreen.ui.navigation.CameraScreenRoute
import com.ascolith.textseek.feature.overview.ui.navigation.OverviewScreenRoute
import com.ascolith.textseek.feature.reportissue.ui.navigation.ReportIssueScreenRoute
import com.ascolith.textseek.feature.settings.ui.navigation.SettingsScreenRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainNavDisplayTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navigator: MainNavigator
    private var editorViewModel: TrackedViewModel? = null

    @Before
    fun setContent() {
        composeTestRule.setContent {
            navigator = rememberMainNavigator()
            MainNavDisplay(
                navigator = navigator,
                entryProvider = entryProvider<NavKey> {
                    topDestinationEntry<CameraScreenRoute> { Counter() }
                    topDestinationEntry<OverviewScreenRoute> { Screen(OVERVIEW) }
                    topDestinationEntry<SettingsScreenRoute> { Screen(SETTINGS) }
                    editorEntry<ReportIssueScreenRoute> {
                        editorViewModel = viewModel { TrackedViewModel() }
                        Screen(EDITOR)
                    }
                },
            )
        }
    }

    @Test
    fun `moving right along the bar slides the next screen in from the right`() {
        val bounds = midTransitionBounds(SETTINGS) { navigator.navigateToTopDestination(TsTopDestination.Settings) }

        assertTrue(bounds.left > 0.dp)
    }

    @Test
    fun `moving left along the bar slides the next screen in from the left`() {
        composeTestRule.runOnIdle { navigator.navigateToTopDestination(TsTopDestination.Settings) }

        val bounds = midTransitionBounds(OVERVIEW) { navigator.navigateToTopDestination(TsTopDestination.Overview) }

        assertTrue(bounds.left < 0.dp)
    }

    @Test
    fun `popping back along the bar slides the previous screen in from the left`() {
        composeTestRule.runOnIdle { navigator.navigateToTopDestination(TsTopDestination.Overview) }

        val bounds = midTransitionBounds(COUNTER) { navigator.goBack() }

        assertTrue(bounds.left < 0.dp)
    }

    @Test
    fun `an editor rises from the bottom`() {
        val bounds = midTransitionBounds(EDITOR) { navigator.open(ReportIssueScreenRoute) }

        assertEquals(0.dp, bounds.left)
        assertTrue(bounds.top > 0.dp)
    }

    @Test
    fun `a closing editor falls away`() {
        composeTestRule.runOnIdle { navigator.open(ReportIssueScreenRoute) }

        val bounds = midTransitionBounds(EDITOR) { navigator.goBack() }

        assertEquals(0.dp, bounds.left)
        assertTrue(bounds.top > 0.dp)
    }

    @Test
    fun `system back closes the screen on top`() {
        composeTestRule.runOnIdle { navigator.open(ReportIssueScreenRoute) }

        composeTestRule.runOnIdle { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeTestRule.runOnIdle {
            assertEquals(listOf(CameraScreenRoute), navigator.routes)
        }
    }

    @Test
    fun `a screen's saved state survives another screen opening over it`() {
        composeTestRule.onNodeWithTag(COUNTER).performClick()
        composeTestRule.runOnIdle { navigator.open(ReportIssueScreenRoute) }
        composeTestRule.onNodeWithTag(EDITOR).assertExists()

        composeTestRule.runOnIdle { navigator.goBack() }

        composeTestRule.onNodeWithTag(COUNTER).assertTextEquals("1")
    }

    @Test
    fun `a screen's view models are cleared once it is popped`() {
        composeTestRule.runOnIdle { navigator.open(ReportIssueScreenRoute) }
        val viewModel = composeTestRule.runOnIdle { checkNotNull(editorViewModel) }
        assertFalse(viewModel.cleared)

        composeTestRule.runOnIdle { navigator.goBack() }

        composeTestRule.runOnIdle { assertTrue(viewModel.cleared) }
    }

    // Starts a transition and returns where [tag] is halfway through it
    private fun midTransitionBounds(tag: String, navigate: () -> Unit): DpRect {
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread(navigate)
        // Frame by frame, idling in between, as the transition is started from a coroutine
        repeat(MID_TRANSITION_FRAMES) {
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.waitForIdle()
        }
        val bounds = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        composeTestRule.mainClock.autoAdvance = true
        return bounds
    }

    class TrackedViewModel : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
        }
    }
}

private const val COUNTER = "counter"
private const val OVERVIEW = "overview"
private const val SETTINGS = "settings"
private const val EDITOR = "editor"
private const val MID_TRANSITION_FRAMES = 12

@Composable
private fun Screen(tag: String) {
    Box(Modifier.fillMaxSize().testTag(tag))
}

// A start screen with saveable state of its own
@Composable
private fun Counter() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    Button(onClick = { count++ }, modifier = Modifier.testTag(COUNTER)) {
        Text("$count")
    }
}
