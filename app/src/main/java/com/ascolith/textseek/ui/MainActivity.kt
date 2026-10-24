package com.ascolith.textseek.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascolith.textseek.feature.camerascreen.ui.model.QuickSearchLaunch
import com.ascolith.textseek.core.ui.TextSeekTheme
import com.ascolith.textseek.feature.onboarding.ui.OnboardingScreen
import com.ascolith.textseek.ui.navigation.MainNavigation
import com.ascolith.textseek.ui.onboarding.OnboardingGateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var quickSearchLaunch by mutableStateOf<QuickSearchLaunch?>(null)

    private val onboardingGateViewModel: OnboardingGateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Held until it's known whether onboarding is needed, so neither screen flashes
        installSplashScreen().setKeepOnScreenCondition { onboardingGateViewModel.completed.value == null }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        quickSearchLaunch = if (savedInstanceState != null) {
            savedInstanceState.getBundle(STATE_QUICK_SEARCH_LAUNCH)?.toQuickSearchLaunch()
        } else {
            intent?.toQuickSearchLaunch()
        }
        setContent {
            TextSeekTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val onboardingCompleted by onboardingGateViewModel.completed.collectAsStateWithLifecycle()
                    Crossfade(targetState = onboardingCompleted) { completed ->
                        when (completed) {
                            // A quick search launch waits here until onboarding is done
                            true -> MainNavigation(quickSearchLaunch = quickSearchLaunch)
                            false -> OnboardingScreen()
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toQuickSearchLaunch()?.let { quickSearchLaunch = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        quickSearchLaunch?.let { outState.putBundle(STATE_QUICK_SEARCH_LAUNCH, it.toBundle()) }
    }

    private fun Intent.toQuickSearchLaunch(): QuickSearchLaunch? {
        if (action != ACTION_QUICK_SEARCH) return null
        return QuickSearchLaunch(
            launchId = resolveLaunchId(),
            query = getStringExtra(EXTRA_QUERY),
            caseSensitive = getBooleanExtra(EXTRA_CASE_SENSITIVE, false),
            colorArgb = if (hasExtra(EXTRA_HIGHLIGHT_COLOR)) {
                getIntExtra(EXTRA_HIGHLIGHT_COLOR, 0)
            } else {
                null
            },
        )
    }

    private fun Intent.resolveLaunchId(): Long {
        if (getLongExtra(EXTRA_LAUNCH_ID, 0L) == 0L) {
            putExtra(EXTRA_LAUNCH_ID, System.nanoTime())
        }
        return getLongExtra(EXTRA_LAUNCH_ID, 0L)
    }

    private fun QuickSearchLaunch.toBundle(): Bundle = Bundle().apply {
        putLong(EXTRA_LAUNCH_ID, launchId)
        putString(EXTRA_QUERY, query)
        putBoolean(EXTRA_CASE_SENSITIVE, caseSensitive)
        colorArgb?.let { putInt(EXTRA_HIGHLIGHT_COLOR, it) }
    }

    private fun Bundle.toQuickSearchLaunch(): QuickSearchLaunch = QuickSearchLaunch(
        launchId = getLong(EXTRA_LAUNCH_ID),
        query = getString(EXTRA_QUERY),
        caseSensitive = getBoolean(EXTRA_CASE_SENSITIVE),
        colorArgb = if (containsKey(EXTRA_HIGHLIGHT_COLOR)) getInt(EXTRA_HIGHLIGHT_COLOR) else null,
    )

    companion object {
        private const val STATE_QUICK_SEARCH_LAUNCH = "com.ascolith.textseek.state.QUICK_SEARCH_LAUNCH"

        internal const val ACTION_QUICK_SEARCH = "com.ascolith.textseek.action.QUICK_SEARCH"
        internal const val EXTRA_QUERY = "com.ascolith.textseek.extra.QUICK_SEARCH_QUERY"
        internal const val EXTRA_CASE_SENSITIVE =
            "com.ascolith.textseek.extra.QUICK_SEARCH_CASE_SENSITIVE"
        internal const val EXTRA_HIGHLIGHT_COLOR =
            "com.ascolith.textseek.extra.QUICK_SEARCH_HIGHLIGHT_COLOR"
        internal const val EXTRA_LAUNCH_ID =
            "com.ascolith.textseek.extra.QUICK_SEARCH_LAUNCH_ID"
    }
}
