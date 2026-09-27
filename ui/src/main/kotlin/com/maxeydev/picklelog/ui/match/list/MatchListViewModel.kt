@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.match.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklelog.domain.match.MatchListItem
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.domain.match.MatchSortStore
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.navigation.JUST_SAVED_MATCH_ID_KEY
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

const val MATCH_LIST_PAGE_SIZE = 50
const val MATCH_LIST_PREFETCH_DISTANCE = 15
private const val STOP_TIMEOUT_MILLIS = 5_000L

class MatchListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val matchSortStore: MatchSortStore,
    private val photoFile: (String) -> File,
    defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val pageLimit = MutableStateFlow(MATCH_LIST_PAGE_SIZE)

    val uiState: StateFlow<MatchListUiState> =
        matchSortStore
            .observeSort()
            .flatMapLatest { sort ->
                pageLimit.flatMapLatest { limit ->
                    matchRepository.observeListPage(sort, limit).map { items ->
                        MatchListUiState(
                            isLoading = false,
                            sort = sort,
                            matches = items.map { it.toRowUiState() },
                            pageLimit = limit,
                        )
                    }
                }
            }.flowOn(defaultDispatcher)
            .combine(savedStateHandle.getStateFlow<String?>(JUST_SAVED_MATCH_ID_KEY, null)) { state, savedMatchId ->
                state.copy(savedMatchId = savedMatchId)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MatchListUiState())

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        val state = uiState.value
        if (!state.canLoadMore) {
            return
        }
        if (lastVisibleIndex < state.matches.size - MATCH_LIST_PREFETCH_DISTANCE) {
            return
        }
        pageLimit.compareAndSet(state.pageLimit, state.pageLimit + MATCH_LIST_PAGE_SIZE)
    }

    fun dismissSavedConfirmation() {
        savedStateHandle[JUST_SAVED_MATCH_ID_KEY] = null
    }

    fun selectSort(sort: MatchSort) {
        pageLimit.value = MATCH_LIST_PAGE_SIZE
        viewModelScope.launch { matchSortStore.saveSort(sort) }
    }

    private fun MatchListItem.toRowUiState(): MatchRowUiState =
        MatchRowUiState(
            id = id.toString(),
            date = date,
            format = format,
            result = result,
            opponentNames = opponentNames,
            games = games,
            thumbnailPath = primaryPhotoPath?.let { photoFile(it).path },
        )

    companion object {
        fun factory(
            dependencies: PicklelogDependencies,
            homeEntryState: SavedStateHandle,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MatchListViewModel(
                        savedStateHandle = homeEntryState,
                        matchRepository = dependencies.matchRepository,
                        matchSortStore = dependencies.matchSortStore,
                        photoFile = dependencies::photoFile,
                        defaultDispatcher = dependencies.defaultDispatcher,
                    )
                }
            }
    }
}
