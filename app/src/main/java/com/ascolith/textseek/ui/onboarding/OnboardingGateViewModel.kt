package com.ascolith.textseek.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascolith.textseek.core.data.onboarding.IOnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Keeps the app behind onboarding until it has been completed. */
@HiltViewModel
class OnboardingGateViewModel @Inject constructor(
    onboardingRepository: IOnboardingRepository,
) : ViewModel() {

    /** Null until the stored value has loaded. */
    val completed: StateFlow<Boolean?> = onboardingRepository.observeCompleted()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
