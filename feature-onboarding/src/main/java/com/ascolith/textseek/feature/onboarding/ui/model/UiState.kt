package com.ascolith.textseek.feature.onboarding.ui.model

import com.ascolith.textseek.domain.model.AppSetting
import com.ascolith.textseek.domain.model.TextRecognitionScript

data class UiState(
    val pages: List<OnboardingPage> = OnboardingPage.entries,
    val currentPage: Int = 0,
    val privacyAgreed: Boolean = false,
    val script: TextRecognitionScript = AppSetting.TextRecognitionScript.defaultValue,
) {
    val page: OnboardingPage get() = pages[currentPage]
    val isLastPage: Boolean get() = currentPage == pages.lastIndex
    val canGoForward: Boolean get() = page != OnboardingPage.PrivacyPolicy || privacyAgreed
}
