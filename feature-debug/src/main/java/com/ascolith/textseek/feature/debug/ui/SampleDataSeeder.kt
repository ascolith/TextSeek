package com.ascolith.textseek.feature.debug.ui

import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroup
import javax.inject.Inject

/**
 * Adds searches that cover each kind of row: grouped, ungrouped, disabled and case sensitive.
 * Nothing is replaced, so seeding again adds another copy.
 */
class SampleDataSeeder @Inject constructor(
    private val searchGroupRepository: ISearchGroupRepository,
    private val searchSettingRepository: ISearchSettingRepository,
) {
    suspend fun seed() {
        val groupId = searchGroupRepository.create(
            name = "Groceries",
            enabled = true,
            colorArgb = SearchGroup.DEFAULT_COLOR_ARGB,
        )
        listOf("Milk", "Eggs", "Bread").forEach { text ->
            searchSettingRepository.create(
                text = text,
                caseSensitive = false,
                color = null,
                enabled = true,
                searchGroupId = groupId,
            )
        }

        searchSettingRepository.create(
            text = "TODO",
            caseSensitive = true,
            color = null,
            enabled = true,
            searchGroupId = null,
        )
        searchSettingRepository.create(
            text = "Draft",
            caseSensitive = false,
            color = null,
            enabled = false,
            searchGroupId = null,
        )
    }
}
