@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class, FlowPreview::class)

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
import com.maxeydev.picklelog.domain.match.FreeTextField
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.match.suggestFreeText
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.person.normalizePersonName
import com.maxeydev.picklelog.domain.person.suggestPeople
import com.maxeydev.picklelog.domain.photo.PhotoImportQueue
import com.maxeydev.picklelog.domain.photo.PhotoImportState
import com.maxeydev.picklelog.domain.photo.PhotoSource
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.common.SuggestionUiState
import com.maxeydev.picklelog.ui.navigation.LOG_ANOTHER_FROM_ARGUMENT
import com.maxeydev.picklelog.ui.navigation.MATCH_ID_ARGUMENT
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val DRAFT_KEY = "match_edit_draft"

private val draftJson = Json { ignoreUnknownKeys = true }

const val SUGGESTION_DEBOUNCE_MILLIS = 150L

private data class SuggestionInput(
    val target: SuggestionTarget,
    val text: String,
    val excludedIds: Set<String>,
    val isDismissed: Boolean,
)

private data class SuggestionResult(
    val target: SuggestionTarget,
    val people: List<Person> = emptyList(),
    val values: List<String> = emptyList(),
)

class MatchEditViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val personRepository: PersonRepository,
    private val lastUsedFormatStore: LastUsedFormatStore,
    private val photoImportQueue: PhotoImportQueue,
    private val photoFile: (String) -> File,
    private val clock: Clock,
    private val timeZone: () -> TimeZone,
    private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var draft: MatchDraft? =
        savedStateHandle.get<String>(DRAFT_KEY)?.let { encoded ->
            draftJson.decodeFromString(MatchDraft.serializer(), encoded)
        }
    private var isSaving = false
    private var isFinished = false
    private var savedNewMatchId: String? = null
    private val suggestionInput = MutableStateFlow<SuggestionInput?>(null)
    private var suggestionResult: SuggestionResult? = null
    private var hasPhotoError = false
    private val photoWatchers = mutableMapOf<String, Job>()

    private val mutableUiState = MutableStateFlow(renderState())
    val uiState: StateFlow<MatchEditUiState> = mutableUiState.asStateFlow()

    init {
        if (draft == null) {
            viewModelScope.launch { loadInitialDraft() }
        } else {
            resumePendingPhotos()
        }
        viewModelScope.launch {
            observeSuggestions().collect { result ->
                suggestionResult = result
                mutableUiState.value = renderState()
            }
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

    fun focusSuggestionTarget(target: SuggestionTarget) {
        val current = draft ?: return
        suggestionInput.value = suggestionInputFor(target, current, isDismissed = false)
        mutableUiState.value = renderState()
    }

    fun leaveSuggestionTarget(target: SuggestionTarget) {
        if (suggestionInput.value?.target != target) {
            return
        }
        suggestionInput.value = null
        mutableUiState.value = renderState()
    }

    fun selectSuggestion(
        target: SuggestionTarget,
        suggestion: SuggestionUiState,
    ) {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        val slot = target.personSlot
        val next =
            if (slot != null) {
                val person =
                    suggestionResult
                        ?.takeIf { it.target == target }
                        ?.people
                        ?.firstOrNull { it.id.toString() == suggestion.key }
                        ?: return
                current.withPersonSelected(slot, person)
            } else {
                when (requireNotNull(target.freeTextField)) {
                    FreeTextField.LOCATION -> current.copy(location = suggestion.label)
                    FreeTextField.PADDLE -> current.copy(paddle = suggestion.label)
                }
            }
        suggestionInput.value = suggestionInputFor(target, next, isDismissed = true)
        publish(next)
    }

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

    fun addPickedPhotos(uris: List<String>) {
        addPhotos(uris.map { PhotoSource(uri = it, isTemporaryCapture = false) })
    }

    fun addCapturedPhoto(uri: String) {
        addPhotos(listOf(PhotoSource(uri = uri, isTemporaryCapture = true)))
    }

    fun removePhoto(key: String) {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        val photo = current.photos.firstOrNull { it.key == key } ?: return
        photoWatchers.remove(key)?.cancel()
        if (!photo.isPersisted) {
            photoImportQueue.discard(key)
        }
        publish(current.withPhotoRemoved(key))
    }

    fun movePhoto(
        key: String,
        offset: Int,
    ) = updateDraft { it.withPhotoMoved(key, offset) }

    fun dismissPhotoError() {
        hasPhotoError = false
        mutableUiState.value = renderState()
    }

    fun save() {
        val current = draft ?: return
        if (!current.canSave() || isSaving || isFinished) {
            return
        }
        isSaving = true
        mutableUiState.value = renderState()
        viewModelScope.launch {
            val matchId = Uuid.parse(current.matchId)
            matchRepository.saveMatch(
                buildMatch(current),
                removedPhotoIds = current.removedPhotoIds.map(Uuid::parse).toSet(),
            )
            current.photos
                .filter { it.isReady && !it.isPersisted }
                .forEach { photoImportQueue.release(it.key) }
            photoImportQueue.attachWhenReady(matchId, current.pendingPhotoKeys())
            if (current.isNew) {
                lastUsedFormatStore.recordLastUsedFormat(current.format)
            }
            isSaving = false
            isFinished = true
            if (current.isNew) {
                savedNewMatchId = current.matchId
            }
            mutableUiState.value = renderState()
        }
    }

    private suspend fun loadInitialDraft() {
        val editingId = savedStateHandle.get<String>(MATCH_ID_ARGUMENT)
        if (editingId == null) {
            publish(newMatchDraft())
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

    private suspend fun newMatchDraft(): MatchDraft {
        val now = clock.now().toLocalDateTime(timeZone())
        val matchId = Uuid.random().toString()
        val startTime = AppTime(now.hour, now.minute)
        val source =
            savedStateHandle.get<String>(LOG_ANOTHER_FROM_ARGUMENT)?.let { sourceId ->
                matchRepository.observeById(Uuid.parse(sourceId)).first()
            }
        return if (source == null) {
            MatchDraft.forNewMatch(
                matchId = matchId,
                format = lastUsedFormatStore.lastUsedFormat(),
                date = now.date,
                startTime = startTime,
            )
        } else {
            logAnotherDraft(source = source, matchId = matchId, startTime = startTime)
        }
    }

    private fun addPhotos(sources: List<PhotoSource>) {
        val current = draft ?: return
        if (isSaving || isFinished || sources.isEmpty()) {
            return
        }
        val added = sources.map(PhotoDraft::importing)
        publish(current.withPhotosAdded(added))
        added.forEach(::startImport)
    }

    private fun resumePendingPhotos() {
        draft
            ?.photos
            ?.filter { !it.isReady && !it.isPersisted }
            ?.forEach(::startImport)
    }

    private fun startImport(photo: PhotoDraft) {
        val source = photo.source ?: return
        photoImportQueue.ensureStarted(photo.key, source)
        photoWatchers[photo.key] =
            viewModelScope.launch {
                val finished = photoImportQueue.observe(photo.key).first { it !is PhotoImportState.Importing }
                photoWatchers.remove(photo.key)
                onImportFinished(photo.key, finished)
            }
    }

    private fun onImportFinished(
        key: String,
        finished: PhotoImportState,
    ) {
        val current = draft ?: return
        if (isSaving || isFinished) {
            return
        }
        when (finished) {
            is PhotoImportState.Ready -> publish(current.withPhotoImported(key, finished.photo))
            else -> {
                photoImportQueue.discard(key)
                hasPhotoError = true
                publish(current.withPhotoRemoved(key))
            }
        }
    }

    override fun onCleared() {
        if (!isFinished) {
            draft
                ?.photos
                ?.filterNot { it.isPersisted }
                ?.forEach { photoImportQueue.discard(it.key) }
        }
        super.onCleared()
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
        suggestionInput.value?.let { input ->
            val refreshed = suggestionInputFor(input.target, next, isDismissed = false)
            suggestionInput.value = refreshed.copy(isDismissed = input.isDismissed && input.text == refreshed.text)
        }
        mutableUiState.value = renderState()
    }

    private fun renderState(): MatchEditUiState =
        draft?.toUiState(
            isSaving = isSaving,
            isFinished = isFinished,
            suggestionTarget = suggestionInput.value?.target,
            suggestions = visibleSuggestions(),
            savedNewMatchId = savedNewMatchId,
            photoFilePath = { photoFile(it).path },
            hasPhotoError = hasPhotoError,
        ) ?: MatchEditUiState(isLoading = !isFinished, isFinished = isFinished)

    private fun visibleSuggestions(): List<SuggestionUiState> {
        val input = suggestionInput.value ?: return emptyList()
        if (input.isDismissed || input.text.isBlank() || isSaving || isFinished) {
            return emptyList()
        }
        val result = suggestionResult?.takeIf { it.target == input.target } ?: return emptyList()
        return result.people.map { SuggestionUiState(key = it.id.toString(), label = it.displayName) } +
            result.values.map { SuggestionUiState(key = it, label = it) }
    }

    private fun suggestionInputFor(
        target: SuggestionTarget,
        source: MatchDraft,
        isDismissed: Boolean,
    ): SuggestionInput {
        val slot = target.personSlot
        return if (slot != null) {
            SuggestionInput(
                target = target,
                text = source.nameIn(slot),
                excludedIds = source.boundIdsOutside(slot) + listOfNotNull(source.idIn(slot)),
                isDismissed = isDismissed,
            )
        } else {
            val text =
                when (requireNotNull(target.freeTextField)) {
                    FreeTextField.LOCATION -> source.location
                    FreeTextField.PADDLE -> source.paddle
                }
            SuggestionInput(target = target, text = text, excludedIds = emptySet(), isDismissed = isDismissed)
        }
    }

    private fun observeSuggestions(): Flow<SuggestionResult?> =
        suggestionInput
            .map { it?.target }
            .distinctUntilChanged()
            .flatMapLatest { target ->
                if (target == null) {
                    flowOf(null)
                } else {
                    suggestionsFor(target)
                }
            }.flowOn(defaultDispatcher)

    private fun suggestionsFor(target: SuggestionTarget): Flow<SuggestionResult> {
        val typed =
            suggestionInput
                .filterNotNull()
                .filter { it.target == target }
                .debounce(SUGGESTION_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
        val field = target.freeTextField
        return if (field == null) {
            combine(personRepository.observeRecentlyUsed(), typed) { usage, input ->
                SuggestionResult(
                    target = target,
                    people =
                        if (input.isDismissed) {
                            emptyList()
                        } else {
                            suggestPeople(input.text, usage, input.excludedIds.map(Uuid::parse).toSet())
                        },
                )
            }
        } else {
            combine(matchRepository.observePriorValues(field), typed) { values, input ->
                SuggestionResult(
                    target = target,
                    values =
                        if (input.isDismissed) {
                            emptyList()
                        } else {
                            suggestFreeText(input.text, values)
                        },
                )
            }
        }
    }

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
            opponents =
                source
                    .visibleSlots()
                    .filter { it != PersonSlot.PARTNER }
                    .mapNotNull { resolvePerson(source, it) },
            partner =
                if (PersonSlot.PARTNER in source.visibleSlots()) {
                    resolvePerson(source, PersonSlot.PARTNER)
                } else {
                    null
                },
            games = source.completeGames(),
            paddle = source.paddle.trim().ifEmpty { null },
            notes = source.notes.trim().ifEmpty { null },
            photos = source.readyPhotoRefs(),
        )
    }

    private suspend fun resolvePerson(
        source: MatchDraft,
        slot: PersonSlot,
    ): Person? {
        val name = source.nameIn(slot)
        if (normalizePersonName(name).isEmpty()) {
            return null
        }
        val bound = source.idIn(slot)?.let { personRepository.findById(Uuid.parse(it)) }
        return bound ?: personRepository.findOrCreatePerson(name)
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
                        photoImportQueue = dependencies.photoImportQueue,
                        photoFile = dependencies::photoFile,
                        clock = dependencies.clock,
                        timeZone = dependencies::currentTimeZone,
                        defaultDispatcher = dependencies.defaultDispatcher,
                    )
                }
            }
    }
}
