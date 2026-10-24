package com.ascolith.textseek.feature.debug

import arrow.core.Either
import arrow.core.right
import com.ascolith.textseek.core.data.log.ICrashRepository
import com.ascolith.textseek.core.data.onboarding.IOnboardingRepository
import com.ascolith.textseek.core.data.searchgroup.ISearchGroupRepository
import com.ascolith.textseek.core.data.searchsetting.ISearchSettingRepository
import com.ascolith.textseek.domain.model.SearchGroup
import com.ascolith.textseek.domain.model.SearchGroupId
import com.ascolith.textseek.domain.model.SearchSetting
import com.ascolith.textseek.domain.model.SearchSettingId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCrashRepository(var pending: Boolean = false) : ICrashRepository {
    override suspend fun hasPendingCrash(): Boolean = pending

    override suspend fun clearPendingCrash() {
        pending = false
    }
}

class FakeOnboardingRepository(completed: Boolean = true) : IOnboardingRepository {
    val completed = MutableStateFlow(completed)

    override fun observeCompleted(): Flow<Boolean> = completed

    override suspend fun markCompleted(): Either<Throwable, Unit> {
        completed.value = true
        return Unit.right()
    }

    override suspend fun reset(): Either<Throwable, Unit> {
        completed.value = false
        return Unit.right()
    }
}

class FakeSearchGroupRepository : ISearchGroupRepository {
    override val searchGroups = MutableStateFlow<List<SearchGroup>>(emptyList())

    override suspend fun create(name: String, enabled: Boolean, colorArgb: Int): SearchGroupId {
        val id = SearchGroupId(searchGroups.value.size + 1L)
        searchGroups.update { it + SearchGroup(id, name, enabled, colorArgb) }
        return id
    }

    override suspend fun get(id: SearchGroupId): SearchGroup? = searchGroups.value.find { it.id == id }
    override suspend fun setGroupEnabled(id: SearchGroupId, enabled: Boolean) = error("unused")
    override suspend fun update(searchGroup: SearchGroup) = error("unused")
    override suspend fun deleteGroupAndChildren(id: SearchGroupId) = error("unused")
}

class FakeSearchSettingRepository : ISearchSettingRepository {
    override val searchSettings = MutableStateFlow<List<SearchSetting>>(emptyList())

    override suspend fun create(
        text: String,
        caseSensitive: Boolean,
        color: Int?,
        enabled: Boolean,
        searchGroupId: SearchGroupId?,
    ): Long {
        val id = searchSettings.value.size + 1L
        searchSettings.update {
            it + SearchSetting(SearchSettingId(id), text, caseSensitive, color, enabled, searchGroupId)
        }
        return id
    }

    override suspend fun get(id: SearchSettingId): SearchSetting? = searchSettings.value.find { it.id == id }

    override suspend fun getInGroup(groupId: SearchGroupId): Flow<List<SearchSetting>> =
        searchSettings.map { settings -> settings.filter { it.searchGroupId == groupId } }

    override suspend fun update(searchSetting: SearchSetting) = error("unused")
    override suspend fun update(id: SearchSettingId, text: String) = error("unused")
    override suspend fun delete(ids: Set<SearchSettingId>) = error("unused")
    override suspend fun delete(id: SearchSettingId) = error("unused")
}
