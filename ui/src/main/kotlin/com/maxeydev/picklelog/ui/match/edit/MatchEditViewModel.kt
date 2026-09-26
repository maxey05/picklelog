@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.match.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.person.normalizePersonName
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.navigation.MATCH_ID_ARGUMENT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val DRAFT_KEY = "match_edit_draft"

private val draftJson = Json { ignoreUnknownKeys = true }

class MatchEditViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val personRepository: PersonRepository,
    private val lastUsedFormatStore: LastUsedFormatStore,
    private val clock: Clock,
    private val timeZone: () -> TimeZone,
) : ViewModel() {
    private var draft: MatchDraft? =
        savedStateHandle.get<String>(DRAFT_KEY)?.let { encoded ->
            draftJson.decodeFromString(MatchDraft.serializer(), encoded)
        }
    private var isSaving = false
    private var isFinished = false

    private val mutableUiState = MutableStateFlow(renderState())
    val uiState: StateFlow<MatchEditUiState> = mutableUiState.asStateFlow()

    init {
        if (draft == null) {
            viewModelScope.launch { loadInitialDraft() }
        }
    }

    fun selectFormat(format: MatchFormat) = updateDraft { it.withFormat(format) }

    fun selectResult(result: MatchResult) = updateDraft { it.copy(result = result) }

    fun selectDate(date: AppDate) = updateDraft { it.copy(date = date.toString()) }

    fun changeStartTime(time: AppTime?) = updateDraft { it.copy(startTime = time?.toString()) }

    fun changeEndTime(time: AppTime?) = updateDraft { it.copy(endTime = time?.toString()) }

    fun changePersonName(
        slot: PersonSlot,
        name: String,
    ) = updateDraft { it.withPersonName(slot, name) }

    fun addGame() = updateDraft { it.withGameAdded() }

    fun removeGame(index: Int) = updateDraft { it.withGameRemoved(index) }

    fun changeGameScores(
        index: Int,
        myScore: String,
        opponentScore: String,
    ) = updateDraft { it.withGameScores(index, myScore, opponentScore) }

    fun changeLocation(location: String) = updateDraft { it.copy(location = location) }

    fun changePaddle(paddle: String) = updateDraft { it.copy(paddle = paddle) }

    fun changeNotes(notes: String) = updateDraft { it.copy(notes = notes) }

    fun save() {
        val current = draft ?: return
        if (!current.canSave() || isSaving || isFinished) {
            return
        }
        isSaving = true
        mutableUiState.value = renderState()
        viewModelScope.launch {
            matchRepository.saveMatch(buildMatch(current))
            if (current.isNew) {
                lastUsedFormatStore.recordLastUsedFormat(current.format)
            }
            isSaving = false
            isFinished = true
            mutableUiState.value = renderState()
        }
    }

    private suspend fun loadInitialDraft() {
        val editingId = savedStateHandle.get<String>(MATCH_ID_ARGUMENT)
        if (editingId == null) {
            val now = clock.now().toLocalDateTime(timeZone())
            publish(
                MatchDraft.forNewMatch(
                    matchId = Uuid.random().toString(),
                    format = lastUsedFormatStore.lastUsedFormat(),
                    date = now.date,
                    startTime = AppTime(now.hour, now.minute),
                ),
            )
            return
        }
        val existing = matchRepository.observeById(Uuid.parse(editingId)).first()
        if (existing == null) {
            isFinished = true
            mutableUiState.value = renderState()
        } else {
            publish(MatchDraft.fromMatch(existing))
        }
    }

    private fun updateDraft(transform: (MatchDraft) -> MatchDraft) {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        publish(transform(current))
    }

    private fun publish(next: MatchDraft) {
        draft = next
        savedStateHandle[DRAFT_KEY] = draftJson.encodeToString(MatchDraft.serializer(), next)
        mutableUiState.value = renderState()
    }

    private fun renderState(): MatchEditUiState =
        draft?.toUiState(isSaving = isSaving, isFinished = isFinished)
            ?: MatchEditUiState(isLoading = !isFinished, isFinished = isFinished)

    private suspend fun buildMatch(source: MatchDraft): Match {
        val id = Uuid.parse(source.matchId)
        val now = clock.now()
        val existing =
            if (source.isNew) {
                null
            } else {
                requireNotNull(matchRepository.observeById(id).first()) {
                    "The match being edited no longer exists."
                }
            }
        return Match(
            id = id,
            format = source.format,
            date = AppDate.parse(source.date),
            result = requireNotNull(source.result) { "A match cannot be saved without a result." },
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            startTime = source.startTime?.let(AppTime::parse),
            endTime = source.endTime?.let(AppTime::parse),
            location = source.location.trim().ifEmpty { null },
            opponents = source.visibleOpponentNames().mapNotNull { resolvePerson(it) },
            partner = source.visiblePartnerName()?.let { resolvePerson(it) },
            games = source.completeGames(),
            paddle = source.paddle.trim().ifEmpty { null },
            notes = source.notes.trim().ifEmpty { null },
            photos = existing?.photos.orEmpty(),
        )
    }

    private suspend fun resolvePerson(name: String): Person? =
        if (normalizePersonName(name).isEmpty()) {
            null
        } else {
            personRepository.findOrCreatePerson(name)
        }

    companion object {
        fun factory(dependencies: PicklelogDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MatchEditViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        matchRepository = dependencies.matchRepository,
                        personRepository = dependencies.personRepository,
                        lastUsedFormatStore = dependencies.lastUsedFormatStore,
                        clock = dependencies.clock,
                        timeZone = dependencies::currentTimeZone,
                    )
                }
            }
    }
}
