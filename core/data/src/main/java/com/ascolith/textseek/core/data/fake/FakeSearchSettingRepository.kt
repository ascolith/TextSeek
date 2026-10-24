package com.ascolith.textseek.core.data.fake

import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeSearchSettingRepository @Inject constructor() : ISearchSettingRepository {
    override val searchSettings: Flow<List<SearchSetting>> = flowOf(fakeSearchSettings)

    override suspend fun create(
        text: String,
        caseSensitive: Boolean,
        color: Int?,
        enabled: Boolean,
        searchGroupId: SearchGroupId?,
    ): Long {
        TODO("Not yet implemented")
    }

    override suspend fun get(id: SearchSettingId): SearchSetting? {
        TODO("Not yet implemented")
    }

    override suspend fun getInGroup(groupId: SearchGroupId): Flow<List<SearchSetting>> {
        TODO("Not yet implemented")
    }

    override suspend fun update(searchSetting: SearchSetting) {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: SearchSettingId, text: String) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(ids: Set<SearchSettingId>) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: SearchSettingId) {
        TODO("Not yet implemented")
    }
}

val fakeSearchSettings = listOf(
    SearchSetting(
        id = SearchSettingId(1),
        text = "One",
        caseSensitive = false,
        color = 0,
        enabled = true,
        searchGroupId = null
    ),
    SearchSetting(
        id = SearchSettingId(2),
        text = "Two",
        caseSensitive = false,
        color = 0,
        enabled = false,
        searchGroupId = null
    ),
    SearchSetting(
        id = SearchSettingId(3),
        text = "Three",
        caseSensitive = false,
        color = 0,
        enabled = true,
        searchGroupId = null
    ),
)
