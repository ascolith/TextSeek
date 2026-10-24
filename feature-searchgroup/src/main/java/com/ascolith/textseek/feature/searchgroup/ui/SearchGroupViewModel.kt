package com.ascolith.textseek.feature.searchgroup.ui

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.composable.TsSearchSettingItemData
import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import com.ascolith.textseek.feature.searchgroup.ui.model.ScreenIntent
import com.ascolith.textseek.feature.searchgroup.ui.model.SideEffect
import com.ascolith.textseek.feature.searchgroup.ui.model.TransientScreen
import com.ascolith.textseek.feature.searchgroup.ui.model.UiState
import com.ascolith.textseek.feature.searchgroup.ui.model.TsDialog
import com.ascolith.textseek.feature.searchgroup.ui.navigation.SearchGroupScreenRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel(assistedFactory = SearchGroupViewModel.Factory::class)
class SearchGroupViewModel @AssistedInject constructor(
    @Assisted private val route: SearchGroupScreenRoute,
    private val searchGroupRepository: ISearchGroupRepository,
    private val searchSettingRepository: ISearchSettingRepository,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(route: SearchGroupScreenRoute): SearchGroupViewModel
    }

    private val searchGroupId = route.id

    override val container = container<UiState, SideEffect>(UiState(transientScreen = TransientScreen.Loading)) {
        if (searchGroupId != null) {
            val group = searchGroupRepository.get(searchGroupId)
            if (group == null) {
                Logger.w { "Search group $searchGroupId no longer exists" }
                reduce { state.copy(transientScreen = TransientScreen.NotFound) }
                // Nothing to observe for a group that is gone.
                return@container
            }
            reduce {
                state.copy(
                    transientScreen = TransientScreen.None,
                    id = group.id,
                    name = group.name,
                    enabled = group.enabled,
                    colorArgb = group.colorArgb,
                )
            }
        } else {
            reduce {
                state.copy(
                    transientScreen = TransientScreen.None,
                )
            }
        }
        observeSettings()
    }

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            is ScreenIntent.DeleteSearchGroup -> showDeleteConfirmation()
            is ScreenIntent.ConfirmDeleteSearchGroup -> deleteSearchGroup()
            is ScreenIntent.UpdateSearchGroupName -> updateSearchGroupName(intent.name)
            is ScreenIntent.ToggleSearchGroupEnabled -> toggleSearchGroupEnabled()
            ScreenIntent.Save -> save()
            ScreenIntent.Exit -> exit()
            is ScreenIntent.ShowDialog -> showDialog(intent.dialog)
            ScreenIntent.HideDialog -> hideDialog()
            is ScreenIntent.UpdateColorPickerSelectedColor -> updateColorPickerSelectedColor(intent.colorArgb)
            ScreenIntent.SaveSearchGroupColor -> saveSearchGroupColor()
            is ScreenIntent.SearchSettingAddClicked -> handleSearchSettingAddClicked(intent.groupId)
            is ScreenIntent.SearchSettingEditClicked -> handleSearchSettingEditClicked(intent.id)
        }
    }

    private fun exit() = intent {
        reduce { state.copy(transientScreen = TransientScreen.Loading) }
        postSideEffect(SideEffect.Exit)
    }

    private fun save() = intent {
        reduce { state.copy(transientScreen = TransientScreen.Loading) }
        val id = state.id
        val name = state.name.trim()
        if (id != null) {
            val updatedSearchGroup = SearchGroup(
                id = id,
                name = name,
                enabled = state.enabled,
                colorArgb = state.colorArgb,
            )
            searchGroupRepository.update(updatedSearchGroup)
        } else {
            searchGroupRepository.create(
                name = name,
                enabled = state.enabled,
                colorArgb = state.colorArgb,
            )
        }

        postSideEffect(SideEffect.Exit)
    }

    private fun updateSearchGroupName(name: String) = intent {
        reduce {
            state.copy(name = name)
        }
    }

    private fun toggleSearchGroupEnabled() = intent {
        reduce {
            val enabled = !state.enabled
            state.copy(enabled = enabled)
        }
    }

    private fun showDeleteConfirmation() = intent {
        reduce {
            state.copy(dialog = TsDialog.DeleteConfirmation)
        }
    }

    private fun deleteSearchGroup() = intent {
        val id = requireNotNull(state.id)
        searchGroupRepository.deleteGroupAndChildren(id)
        postSideEffect(SideEffect.Exit)
    }

    private fun showDialog(dialog: TsDialog) = intent {
        reduce {
            state.copy(dialog = dialog)
        }
    }

    private fun hideDialog() = intent {
        reduce {
            state.copy(dialog = TsDialog.None)
        }
    }

    private fun updateColorPickerSelectedColor(colorArgb: Int?) = intent {
        reduce {
            state.copy(
                dialog = when (val dialog = state.dialog) {
                    is TsDialog.ColorPicker -> dialog.copy(selectedColorArgb = colorArgb)
                    else -> state.dialog
                }
            )
        }
    }

    private fun saveSearchGroupColor() = intent {
        val colorArgb = when (val dialog = state.dialog) {
            is TsDialog.ColorPicker -> dialog.selectedColorArgb ?: state.colorArgb
            else -> state.colorArgb
        }
        reduce {
            state.copy(colorArgb = colorArgb, dialog = TsDialog.None)
        }
    }

    private fun handleSearchSettingAddClicked(groupId: SearchGroupId) = intent {
        postSideEffect(SideEffect.NavigateToSearchSettingAdd(groupId))
    }

    private fun handleSearchSettingEditClicked(id: SearchSettingId) = intent {
        postSideEffect(SideEffect.NavigateToSearchSettingEdit(id))
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun observeSettings() = subIntent {
        // A group that has not been saved yet has no children to observe.
        if (searchGroupId == null) return@subIntent

        repeatOnSubscription {
            searchSettingRepository.getInGroup(searchGroupId)
                .collect { searchSettings ->
                    reduce {
                        state.copy(
                            searchSettings = searchSettings
                                .map { it.toTsSearchSettingItemData() }
                                .associateBy { it.searchSettingId }
                        )
                    }
                }
        }
    }

    private fun SearchSetting.toTsSearchSettingItemData() =
        TsSearchSettingItemData(
            searchSettingId = id,
            text = text,
            enabled = enabled,
            colorArgb = color,
        )
}
