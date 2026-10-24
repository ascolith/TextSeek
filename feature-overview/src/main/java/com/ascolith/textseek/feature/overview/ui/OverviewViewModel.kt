package com.ascolith.textseek.feature.overview.ui

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.overview.ui.model.OverviewItemUiModel
import com.ascolith.textseek.feature.overview.ui.model.ScreenIntent
import com.ascolith.textseek.feature.overview.ui.model.SideEffect
import com.ascolith.textseek.feature.overview.ui.model.TransientScreen
import com.ascolith.textseek.feature.overview.ui.model.UiState
import com.ascolith.textseek.feature.overview.ui.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val searchGroupRepository: ISearchGroupRepository,
    private val searchSettingRepository: ISearchSettingRepository,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    override val container = container<UiState, SideEffect>(UiState(transientScreen = TransientScreen.Loading)) {
        intent { observeSettingsAndGroups() }
    }

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            is ScreenIntent.SearchSettingAddClicked -> handleSearchSettingAddClicked(intent.groupId)
            is ScreenIntent.SearchSettingEditClicked -> handleSearchSettingEditClicked(intent.id)
            ScreenIntent.SearchGroupAddClicked -> handleSearchGroupAddClicked()
            is ScreenIntent.SearchGroupEditClicked -> handleSearchGroupEditClicked(intent.id)
            is ScreenIntent.ToggleSearchGroupEnabled -> toggleSearchGroupEnabled(intent.id)
        }
    }

    private fun handleSearchGroupAddClicked() = intent {
        postSideEffect(SideEffect.NavigateToSearchGroupAdd)
    }


    private fun handleSearchSettingAddClicked(groupId: SearchGroupId) = intent {
        postSideEffect(SideEffect.NavigateToSearchSettingAdd(groupId))
    }

    private fun handleSearchSettingEditClicked(id: SearchSettingId) = intent {
        postSideEffect(SideEffect.NavigateToSearchSettingEdit(id))
    }

    private fun handleSearchGroupEditClicked(id: SearchGroupId) = intent {
        postSideEffect(SideEffect.NavigateToSearchGroupEdit(id))
    }

    private fun toggleSearchGroupEnabled(id: SearchGroupId) = intent {
        val searchGroup = state.overviewItems[id]?.group ?: run {
            Logger.w { "Toggle requested for search group $id, which is no longer shown" }
            return@intent
        }
        searchGroupRepository.setGroupEnabled(id, enabled = !searchGroup.enabled)
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun observeSettingsAndGroups() = subIntent {
        repeatOnSubscription {
            combine(
                searchGroupRepository.searchGroups,
                searchSettingRepository.searchSettings,
            ) { searchGroups, searchSettings ->

                val settingsMap = searchSettings
                    .map { it.toUiModel() }
                    .groupBy { it.searchGroupId }

                settingsMap[null]?.let { ungrouped ->
                    Logger.w {
                        "${ungrouped.size} search setting(s) have no group and are not shown: " +
                            ungrouped.map { it.id }
                    }
                }

                searchGroups
                    .associate { group ->
                        group.id to OverviewItemUiModel(
                            group = group.toUiModel(),
                            settings = settingsMap[group.id]?.associateBy { it.id } ?: emptyMap(),
                        )
                    }
            }.collect { overviewItems ->
                reduce {
                    state.copy(
                        transientScreen = TransientScreen.None,
                        overviewItems = overviewItems,
                    )
                }
            }
        }
    }
}
