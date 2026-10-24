package com.ascolith.textseek.feature.searchsetting.ui

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.feature.searchsetting.ui.model.ScreenIntent
import com.ascolith.textseek.feature.searchsetting.ui.model.SelectedColor
import com.ascolith.textseek.feature.searchsetting.ui.model.SideEffect
import com.ascolith.textseek.feature.searchsetting.ui.model.TransientScreen
import com.ascolith.textseek.feature.searchsetting.ui.model.UiState
import com.ascolith.textseek.feature.searchsetting.ui.model.TsDialog
import com.ascolith.textseek.feature.searchsetting.ui.navigation.SearchSettingScreenRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel(assistedFactory = SearchSettingViewModel.Factory::class)
class SearchSettingViewModel @AssistedInject constructor(
    @Assisted private val route: SearchSettingScreenRoute,
    private val searchSettingRepository: ISearchSettingRepository,
    private val searchGroupRepository: ISearchGroupRepository,
) : ContainerHost<UiState, SideEffect>, ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(route: SearchSettingScreenRoute): SearchSettingViewModel
    }

    private val searchSettingId = route.id
    private val groupIdDefault = route.groupIdDefault

    override val container = container<UiState, SideEffect>(UiState(transientScreen = TransientScreen.Loading)) {
        val searchGroups = searchGroupRepository.searchGroups.first()

        if (searchSettingId != null) {
            val setting = searchSettingRepository.get(searchSettingId)
            if (setting == null) {
                Logger.w { "Search setting $searchSettingId no longer exists" }
                reduce { state.copy(transientScreen = TransientScreen.NotFound) }
                return@container
            }
            val selectedSearchGroup = setting.searchGroupId?.let { groupId ->
                searchGroupRepository.get(groupId)
            }

            reduce {
                state.copy(
                    transientScreen = TransientScreen.None,
                    searchGroups = searchGroups,
                    selectedSearchGroup = selectedSearchGroup,
                    id = searchSettingId,
                    text = setting.text,
                    caseSensitive = setting.caseSensitive,
                    selectedColor = SelectedColor(
                        settingColorArgb = setting.color,
                        groupColorArgb = selectedSearchGroup?.colorArgb,
                    ),
                    enabled = setting.enabled,
                )
            }

        } else {
            val selectedSearchGroup = searchGroups.firstOrNull { it.id == groupIdDefault }
                ?: searchGroups.firstOrNull()
            reduce {
                state.copy(
                    transientScreen = TransientScreen.None,
                    searchGroups = searchGroups,
                    selectedSearchGroup = selectedSearchGroup,
                    selectedColor = SelectedColor(groupColorArgb = selectedSearchGroup?.colorArgb),
                )
            }
        }
    }

    fun handleIntent(intent: ScreenIntent) {
        when (intent) {
            is ScreenIntent.DeleteSearchSetting -> showDeleteConfirmation()
            is ScreenIntent.ConfirmDeleteSearchSetting -> deleteSearchSetting()
            is ScreenIntent.SaveSearchSettingColor -> saveSearchSettingColor()
            ScreenIntent.UseGroupColor -> useGroupColor()
            is ScreenIntent.UpdateSearchSettingText -> updateSearchSettingText(intent.text)
            is ScreenIntent.ToggleSearchSettingCaseSensitive -> toggleSearchSettingCaseSensitive()
            is ScreenIntent.ToggleSearchSettingEnabled -> toggleSearchSettingEnabled()
            is ScreenIntent.UpdateSearchSettingGroup -> updateSearchSettingGroup(intent.searchGroupId)
            ScreenIntent.Save -> save()
            ScreenIntent.Exit -> exit()
            is ScreenIntent.ShowDialog -> showDialog(intent.dialog)
            ScreenIntent.HideDialog -> hideDialog()
            is ScreenIntent.UpdateColorPickerSelectedColor -> updateColorPickerSelectedColor(intent.colorArgb)
        }
    }

    private fun exit() = intent {
        reduce { state.copy(transientScreen = TransientScreen.Loading) }
        postSideEffect(SideEffect.Exit)
    }

    private fun save() = intent {
        reduce { state.copy(transientScreen = TransientScreen.Loading) }
        val id = state.id
        if (id != null) {
            val updatedSetting = SearchSetting(
                id = id,
                text = state.text.trim(),
                caseSensitive = state.caseSensitive,
                color = state.selectedColor.settingColorArgb,
                enabled = state.enabled,
                searchGroupId = state.selectedSearchGroup?.id,
            )
            searchSettingRepository.update(updatedSetting)
        } else {
            with(state) {
                searchSettingRepository.create(
                    text = text.trim(),
                    caseSensitive = caseSensitive,
                    color = state.selectedColor.settingColorArgb,
                    enabled = enabled,
                    searchGroupId = selectedSearchGroup?.id,
                )
            }
        }
        postSideEffect(SideEffect.Exit)
    }

    private fun updateSearchSettingText(text: String) = intent {
        reduce {
            state.copy(text = text)
        }
    }

    private fun saveSearchSettingColor() = intent {
        val colorArgb = (state.dialog as? TsDialog.ColorPicker)?.selectedColorArgb
            ?: state.selectedColor.settingColorArgb
        reduce {
            state.copy(
                selectedColor = state.selectedColor.copy(settingColorArgb = colorArgb),
                dialog = TsDialog.None,
            )
        }
    }

    private fun useGroupColor() = intent {
        reduce {
            state.copy(
                selectedColor = state.selectedColor.copy(settingColorArgb = null),
                dialog = TsDialog.None,
            )
        }
    }

    private fun toggleSearchSettingCaseSensitive() = intent {
        reduce {
            val newValue = !state.caseSensitive
            state.copy(caseSensitive = newValue)
        }
    }

    private fun toggleSearchSettingEnabled() = intent {
        reduce {
            val enabled = !state.enabled
            state.copy(enabled = enabled)
        }
    }

    private fun showDeleteConfirmation() = intent {
        reduce { state.copy(dialog = TsDialog.DeleteConfirmation) }
    }

    private fun deleteSearchSetting() = intent {
        val id = requireNotNull(state.id)
        searchSettingRepository.delete(id)
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

    private fun updateColorPickerSelectedColor(colorArgb: Int) = intent {
        reduce {
            state.copy(
                dialog = TsDialog.ColorPicker(
                    selectedColorArgb = colorArgb
                )
            )
        }
    }

    private fun updateSearchSettingGroup(searchGroupId: SearchGroupId?) = intent {
        val group = searchGroupId?.let { groupId ->
            state.searchGroups.find { it.id == groupId }
        }
        reduce {
            state.copy(
                selectedSearchGroup = group,
                selectedColor = state.selectedColor.copy(groupColorArgb = group?.colorArgb),
                dialog = TsDialog.None,
            )
        }
    }
}
