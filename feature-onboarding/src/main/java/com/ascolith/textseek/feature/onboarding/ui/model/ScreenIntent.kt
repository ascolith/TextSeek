package com.ascolith.textseek.feature.onboarding.ui.model

import com.ascolith.textseek.domain.model.TextRecognitionScript

sealed interface ScreenIntent {
    data object Back : ScreenIntent
    data object Forward : ScreenIntent
    data class SetPrivacyAgreed(val agreed: Boolean) : ScreenIntent
    data class SelectScript(val script: TextRecognitionScript) : ScreenIntent
}
