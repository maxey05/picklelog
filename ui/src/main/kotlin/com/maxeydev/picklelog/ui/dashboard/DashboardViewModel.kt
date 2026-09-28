@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.profile.UserProfile
import com.maxeydev.picklelog.domain.stats.BasicStats
import com.maxeydev.picklelog.domain.stats.MatchStatLine
import com.maxeydev.picklelog.domain.streak.StreakEngine
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.match.list.observeHomeFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.uuid.ExperimentalUuidApi

private const val STOP_TIMEOUT_MILLIS = 5_000L

private data class FilteredStats(
    val filter: FilterState,
    val stats: BasicStats,
)

class DashboardViewModel(
    homeEntryState: SavedStateHandle,
    matchRepository: MatchRepository,
    personRepository: PersonRepository,
    profileRepository: ProfileRepository,
    streakEngine: StreakEngine,
    defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val filteredStats: Flow<FilteredStats> =
        observeHomeFilter(homeEntryState)
            .distinctUntilChanged()
            .flatMapLatest { filter ->
                matchRepository.observeStatLines(filter).map { lines ->
                    FilteredStats(filter = filter, stats = BasicStats.from(lines))
                }
            }

    private val wholeHistory: Flow<List<MatchStatLine>> = matchRepository.observeStatLines(FilterState.NONE)

    val uiState: StateFlow<DashboardUiState> =
        combine(
            filteredStats,
            wholeHistory,
            profileRepository.observeProfile(),
            personRepository.observeAll(),
        ) { filtered: FilteredStats, history: List<MatchStatLine>, profile: UserProfile, people: List<Person> ->
            DashboardUiState(
                isLoading = false,
                displayName = profile.displayName.trim(),
                stats = filtered.stats,
                streak = streakEngine.compute(history.map { it.date }),
                hasAnyMatches = history.isNotEmpty(),
                filter = filtered.filter,
                filteredOpponentName =
                    filtered.filter.opponentId?.let { id -> people.firstOrNull { it.id == id }?.displayName },
                isPro = profile.entitlement.isPro,
            )
        }.flowOn(defaultDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardUiState())

    companion object {
        fun factory(
            dependencies: PicklelogDependencies,
            homeEntryState: SavedStateHandle,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    DashboardViewModel(
                        homeEntryState = homeEntryState,
                        matchRepository = dependencies.matchRepository,
                        personRepository = dependencies.personRepository,
                        profileRepository = dependencies.profileRepository,
                        streakEngine = StreakEngine(dependencies.clock, dependencies::currentTimeZone),
                        defaultDispatcher = dependencies.defaultDispatcher,
                    )
                }
            }
    }
}
