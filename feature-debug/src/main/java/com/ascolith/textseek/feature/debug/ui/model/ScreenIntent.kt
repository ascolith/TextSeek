package com.ascolith.textseek.feature.debug.ui.model

sealed interface ScreenIntent {
    data object ForceCrash : ScreenIntent
    data object ClearPendingCrash : ScreenIntent
    data object SeedSampleData : ScreenIntent
    data object ResetOnboarding : ScreenIntent
}
