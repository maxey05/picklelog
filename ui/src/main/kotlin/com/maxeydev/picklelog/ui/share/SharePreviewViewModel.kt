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
import com.maxeydev.picklelog.domain.share.CardFormat
import com.maxeydev.picklelog.domain.share.CardFormatStore
import com.maxeydev.picklelog.domain.share.CardLayout
import com.maxeydev.picklelog.domain.share.CardRatio
import com.maxeydev.picklelog.domain.share.CardTheme
import com.maxeydev.picklelog.domain.streak.InsuredStreakEngine
import com.maxeydev.picklelog.domain.streak.streakInsuranceStart
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.navigation.MATCH_ID_ARGUMENT
import com.maxeydev.picklelog.ui.paywall.UpgradeReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    private val formatStore: CardFormatStore,
    private val streakEngine: InsuredStreakEngine,
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

    private var renderJob: Job? = null

    init {
        viewModelScope.launch {
            render(formatStore.observeFormat().first())
        }
    }

    fun reportShareFailed() {
        mutableUiState.update { it.copy(hasShareFailed = true) }
    }

    fun retry() {
        if (mutableUiState.value.isRendering) {
            return
        }
        render(mutableUiState.value.format)
    }

    fun selectRatio(ratio: CardRatio) {
        changeFormat(mutableUiState.value.format.copy(ratio = ratio))
    }

    fun selectTheme(theme: CardTheme) {
        viewModelScope.launch {
            val isPro = profileRepository.observeProfile().first().entitlement.isPro
            if (theme.requiresPro && !isPro) {
                mutableUiState.update { it.copy(upgradeReason = UpgradeReason.PRO_THEME) }
            } else {
                changeFormat(mutableUiState.value.format.copy(theme = theme))
            }
        }
    }

    fun dismissUpgrade() {
        mutableUiState.update { it.copy(upgradeReason = null) }
    }

    fun selectLayout(layout: CardLayout) {
        val override = if (layout == CardLayout.NO_PHOTO) CardLayout.NO_PHOTO else null
        changeFormat(mutableUiState.value.format.copy(layoutOverride = override))
    }

    private fun changeFormat(format: CardFormat) {
        if (format == mutableUiState.value.format) {
            return
        }
        viewModelScope.launch { formatStore.saveFormat(format) }
        render(format)
    }

    private fun render(format: CardFormat) {
        renderJob?.cancel()
        mutableUiState.update {
            it.copy(isRendering = true, hasFailed = false, hasShareFailed = false, format = format)
        }
        renderJob =
            viewModelScope.launch {
                val match = matchRepository.observeById(matchId).first()
                if (match == null) {
                    mutableUiState.update { it.copy(isRendering = false, isGone = true) }
                    return@launch
                }
                val profile = profileRepository.observeProfile().first()
                val history = matchRepository.observeStatLines(FilterState.NONE).first()
                val isPro = profile.entitlement.isPro
                val shown = format.forEntitlement(isPro)
                val insuranceStart = profile.entitlement.streakInsuranceStart()
                val streak = streakEngine.compute(history.map { it.date }, insuranceStart).streak
                val primary = match.photos.primaryPhoto()
                val layout = shown.layoutFor(hasPhoto = primary != null)
                val photo =
                    primary
                        ?.takeIf { layout == CardLayout.PHOTO }
                        ?.let { loadPhotoDataUri(photoFile(it.relativePath)) }
                val data =
                    buildCardData(match, profile.displayName, streak, photo, labels, shown, showWordmark = !isPro)
                val result = renderer.render(data)
                mutableUiState.update { current ->
                    when (result) {
                        is CardRenderResult.Rendered ->
                            current.copy(
                                isRendering = false,
                                format = shown,
                                isPro = isPro,
                                card = result.bitmap,
                                cardDescription = describe(data),
                                hasPhoto = primary != null,
                            )
                        is CardRenderResult.Failed ->
                            current.copy(
                                isRendering = false,
                                card = null,
                                hasFailed = true,
                                hasPhoto = primary != null,
                                format = shown,
                                isPro = isPro,
                            )
                    }
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
                        formatStore = dependencies.cardFormatStore,
                        streakEngine = InsuredStreakEngine(dependencies.clock, dependencies::currentTimeZone),
                        photoFile = dependencies::photoFile,
                        renderer = dependencies.cardRenderer,
                        labels = labels,
                        ioDispatcher = dependencies.ioDispatcher,
                    )
                }
            }
    }
}
