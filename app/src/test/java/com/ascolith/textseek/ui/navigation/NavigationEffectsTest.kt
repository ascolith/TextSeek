package com.ascolith.textseek.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import com.ascolith.textseek.core.model.ScaffoldConfig
import com.ascolith.textseek.core.model.TsScaffoldState
import com.ascolith.textseek.core.ui.TsTopDestination
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchLaunch
import com.ascolith.textseek.feature.camerascreen.ui.navigation.CameraScreenRoute
import com.ascolith.textseek.feature.reportissue.ui.navigation.ReportIssueScreenRoute
import com.ascolith.textseek.feature.settings.ui.navigation.SettingsScreenRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationEffectsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navigator: MainNavigator

    @Test
    fun `the navigator starts on the start destination`() {
        composeTestRule.setContent { navigator = rememberMainNavigator() }

        composeTestRule.runOnIdle {
            assertEquals(listOf(startDestination.toNavKey()), navigator.routes)
        }
    }

    @Test
    fun `the navigator's back stack survives recreation`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { navigator = rememberMainNavigator() }
        composeTestRule.runOnIdle {
            navigator.navigateToTopDestination(TsTopDestination.Settings)
            navigator.open(ReportIssueScreenRoute)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle {
            assertEquals(listOf(CameraScreenRoute, SettingsScreenRoute, ReportIssueScreenRoute), navigator.routes)
        }
    }

    @Test
    fun `the scaffold takes the config of the screen on top`() {
        val scaffoldState = TsScaffoldState()
        composeTestRule.setContent { ScaffoldOwnerEffect(scaffoldState, SettingsScreenRoute) }

        composeTestRule.runOnIdle {
            assertEquals(SettingsScreenRoute.getScaffoldConfig(), scaffoldState.config)
        }
    }

    @Test
    fun `a screen without a config of its own gets the default`() {
        val scaffoldState = TsScaffoldState()
        var currentRoute by mutableStateOf<NavKey>(SettingsScreenRoute)
        composeTestRule.setContent { ScaffoldOwnerEffect(scaffoldState, currentRoute) }

        composeTestRule.runOnIdle { currentRoute = CameraScreenRoute }

        composeTestRule.runOnIdle {
            assertEquals(ScaffoldConfig(), scaffoldState.config)
        }
    }

    @Test
    fun `the previous screen's scaffold listeners are dropped when another takes over`() {
        val scaffoldState = TsScaffoldState()
        var currentRoute by mutableStateOf<NavKey>(SettingsScreenRoute)
        composeTestRule.setContent { ScaffoldOwnerEffect(scaffoldState, currentRoute) }
        composeTestRule.runOnIdle {
            scaffoldState.updateFabClickListener(SettingsScreenRoute) {}
            scaffoldState.updateActionClickListener(SettingsScreenRoute) {}
        }

        composeTestRule.runOnIdle { currentRoute = ReportIssueScreenRoute }

        composeTestRule.runOnIdle {
            assertNull(scaffoldState.fabClickListener)
            assertNull(scaffoldState.actionClickListener)
        }
    }

    @Test
    fun `no quick search launch leaves the back stack alone`() {
        composeTestRule.setContent {
            navigator = rememberMainNavigator()
            QuickSearchLaunchEffect(quickSearchLaunch = null, navigator = navigator)
        }

        composeTestRule.runOnIdle {
            navigator.navigateToTopDestination(TsTopDestination.Settings)
        }

        composeTestRule.runOnIdle {
            assertEquals(listOf(CameraScreenRoute, SettingsScreenRoute), navigator.routes)
        }
    }

    @Test
    fun `a quick search launch pops back to the camera`() {
        var launch by mutableStateOf<QuickSearchLaunch?>(null)
        composeTestRule.setContent {
            navigator = rememberMainNavigator()
            QuickSearchLaunchEffect(launch, navigator)
        }
        composeTestRule.runOnIdle {
            navigator.navigateToTopDestination(TsTopDestination.Settings)
            navigator.open(ReportIssueScreenRoute)
        }

        composeTestRule.runOnIdle { launch = QuickSearchLaunch(launchId = 1) }

        composeTestRule.runOnIdle {
            assertEquals(listOf(CameraScreenRoute), navigator.routes)
        }
    }

    @Test
    fun `a new quick search launch pops back again`() {
        var launch by mutableStateOf<QuickSearchLaunch?>(QuickSearchLaunch(launchId = 1))
        composeTestRule.setContent {
            navigator = rememberMainNavigator()
            QuickSearchLaunchEffect(launch, navigator)
        }
        composeTestRule.runOnIdle { navigator.navigateToTopDestination(TsTopDestination.Settings) }

        composeTestRule.runOnIdle { launch = QuickSearchLaunch(launchId = 2) }

        composeTestRule.runOnIdle {
            assertEquals(listOf(CameraScreenRoute), navigator.routes)
        }
    }

    @Test
    fun `a quick search launch handed back after recreation doesn't pop the restored back stack`() {
        val launch = QuickSearchLaunch(launchId = 1)
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            navigator = rememberMainNavigator()
            QuickSearchLaunchEffect(launch, navigator)
        }
        composeTestRule.runOnIdle { navigator.navigateToTopDestination(TsTopDestination.Settings) }

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.runOnIdle {
            assertEquals(listOf(CameraScreenRoute, SettingsScreenRoute), navigator.routes)
        }
    }
}
