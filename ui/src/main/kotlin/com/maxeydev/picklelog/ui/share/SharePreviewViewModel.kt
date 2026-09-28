@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.share

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.photo.primaryPhoto
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.streak.StreakEngine
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.navigation.MATCH_ID_ARGUMENT
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SharePreviewViewModel(
    savedStateHandle: SavedStateHandle,
    private val matchRepository: MatchRepository,
    private val profileRepository: ProfileRepository,
    private val streakEngine: StreakEngine,
    private val photoFile: (String) -> File,
    private val renderer: CardRendering,
    private val labels: CardLabels,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val matchId: Uuid =
        Uuid.parse(
            requireNotNull(savedStateHandle.get<String>(MATCH_ID_ARGUMENT)) {
                "The share screen was opened without a match id."
            },
        )

    private val mutableUiState = MutableStateFlow(SharePreviewUiState())
    val uiState: StateFlow<SharePreviewUiState> = mutableUiState.asStateFlow()

    init {
        render()
    }

    fun reportShareFailed() {
        mutableUiState.value = mutableUiState.value.copy(hasShareFailed = true)
    }

    fun retry() {
        if (mutableUiState.value.isRendering) {
            return
        }
        render()
    }

    private fun render() {
        mutableUiState.value = SharePreviewUiState(isRendering = true)
        viewModelScope.launch {
            val match = matchRepository.observeById(matchId).first()
            if (match == null) {
                mutableUiState.value = SharePreviewUiState(isRendering = false, isGone = true)
                return@launch
            }
            val profile = profileRepository.observeProfile().first()
            val history = matchRepository.observeStatLines(FilterState.NONE).first()
            val streak = streakEngine.compute(history.map { it.date })
            val photo = match.photos.primaryPhoto()?.let { loadPhotoDataUri(photoFile(it.relativePath)) }
            val data = buildCardData(match, profile.displayName, streak, photo, labels)
            mutableUiState.value =
                when (val result = renderer.render(data)) {
                    is CardRenderResult.Rendered ->
                        SharePreviewUiState(
                            isRendering = false,
                            card = result.bitmap,
                            cardDescription = describe(data),
                        )
                    is CardRenderResult.Failed -> SharePreviewUiState(isRendering = false, hasFailed = true)
                }
        }
    }

    private suspend fun loadPhotoDataUri(file: File): String? =
        withContext(ioDispatcher) {
            try {
                "data:image/jpeg;base64," + Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            } catch (missing: IOException) {
                null
            }
        }

    private fun describe(data: CardData): String =
        listOfNotNull(
            data.displayName.takeIf { it.isNotEmpty() },
            data.result,
            data.meta,
            data.opponents,
            data.partner,
            data.score,
            data.location,
            data.streak,
        ).joinToString(separator = ". ")

    companion object {
        fun factory(
            dependencies: PicklelogDependencies,
            labels: CardLabels,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SharePreviewViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        matchRepository = dependencies.matchRepository,
                        profileRepository = dependencies.profileRepository,
                        streakEngine = StreakEngine(dependencies.clock, dependencies::currentTimeZone),
                        photoFile = dependencies::photoFile,
                        renderer = dependencies.cardRenderer,
                        labels = labels,
                        ioDispatcher = dependencies.ioDispatcher,
                    )
                }
            }
    }
}
