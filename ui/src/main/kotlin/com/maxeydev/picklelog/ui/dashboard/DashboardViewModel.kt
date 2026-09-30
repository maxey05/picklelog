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
import com.maxeydev.picklelog.domain.stats.AdvancedStats
import com.maxeydev.picklelog.domain.stats.BasicStats
import com.maxeydev.picklelog.domain.stats.MatchStatLine
import com.maxeydev.picklelog.domain.streak.InsuredStreakEngine
import com.maxeydev.picklelog.domain.streak.MissedSkipOpportunity
import com.maxeydev.picklelog.domain.streak.SkipNotices
import com.maxeydev.picklelog.domain.streak.StreakNoticeState
import com.maxeydev.picklelog.domain.streak.StreakNoticeStore
import com.maxeydev.picklelog.domain.streak.streakInsuranceStart
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
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

private const val STOP_TIMEOUT_MILLIS = 5_000L

private data class FilteredStats(
    val filter: FilterState,
    val stats: BasicStats,
)

private fun StreakNoticeState.hides(opportunity: MissedSkipOpportunity): Boolean =
    SkipNotices.isMissedOpportunityAcknowledged(opportunity, acknowledgedMissedWeek)

class DashboardViewModel(
    homeEntryState: SavedStateHandle,
    matchRepository: MatchRepository,
    personRepository: PersonRepository,
    profileRepository: ProfileRepository,
    private val streakEngine: InsuredStreakEngine,
    private val streakNoticeStore: StreakNoticeStore,
    defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val homeFilter: Flow<FilterState> = observeHomeFilter(homeEntryState).distinctUntilChanged()

    private val filteredStats: Flow<FilteredStats> =
        homeFilter.flatMapLatest { filter ->
            matchRepository.observeStatLines(filter).map { lines ->
                FilteredStats(filter = filter, stats = BasicStats.from(lines))
            }
        }

    private val wholeHistory: Flow<List<MatchStatLine>> = matchRepository.observeStatLines(FilterState.NONE)

    private val advancedStats: Flow<AdvancedStats> =
        homeFilter.flatMapLatest { filter ->
            matchRepository.observeAdvancedLines(filter).map { lines ->
                AdvancedStats.from(lines, streakEngine.today())
            }
        }

    private val baseState: Flow<DashboardUiState> =
        combine(
            filteredStats,
            wholeHistory,
            profileRepository.observeProfile(),
            personRepository.observeAll(),
            streakNoticeStore.observe(),
        ) {
                filtered: FilteredStats,
                history: List<MatchStatLine>,
                profile: UserProfile,
                people: List<Person>,
                notices: StreakNoticeState,
            ->
            val dates = history.map { it.date }
            val insured = streakEngine.compute(dates, profile.entitlement.streakInsuranceStart())
            val today = streakEngine.today()
            val isPro = profile.entitlement.isPro
            val missed =
                if (isPro) null else SkipNotices.findMissedOpportunity(dates, today)?.takeUnless(notices::hides)
            DashboardUiState(
                isLoading = false,
                displayName = profile.displayName.trim(),
                stats = filtered.stats,
                streak = insured.streak,
                hasAnyMatches = history.isNotEmpty(),
                filter = filtered.filter,
                filteredOpponentName =
                    filtered.filter.opponentId?.let { id -> people.firstOrNull { it.id == id }?.displayName },
                isPro = isPro,
                peopleNames = people.associate { it.id to it.displayName },
                skipsHeld = if (isPro) insured.skipsHeld else 0,
                usedSkipWeek =
                    if (isPro) {
                        SkipNotices.unacknowledgedSkip(insured, today, notices.acknowledgedSkipWeek)
                    } else {
                        null
                    },
                missedOpportunity = missed,
            )
        }

    val uiState: StateFlow<DashboardUiState> =
        combine(baseState, advancedStats) { state, advanced -> state.copy(advanced = advanced) }
            .flowOn(defaultDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardUiState())

    fun dismissUsedSkip() {
        val week = uiState.value.usedSkipWeek ?: return
        viewModelScope.launch { streakNoticeStore.acknowledgeSkip(week.ordinal) }
    }

    fun dismissMissedOpportunity() {
        val opportunity = uiState.value.missedOpportunity ?: return
        viewModelScope.launch { streakNoticeStore.acknowledgeMissed(opportunity.missedWeek.ordinal) }
    }

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
                        streakEngine = InsuredStreakEngine(dependencies.clock, dependencies::currentTimeZone),
                        streakNoticeStore = dependencies.streakNoticeStore,
                        defaultDispatcher = dependencies.defaultDispatcher,
                    )
                }
            }
    }
}
