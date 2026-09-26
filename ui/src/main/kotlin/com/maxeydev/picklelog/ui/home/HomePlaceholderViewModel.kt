@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.ui.PicklelogDependencies
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.uuid.ExperimentalUuidApi

private const val STOP_TIMEOUT_MILLIS = 5_000L

class HomePlaceholderViewModel(
    matchRepository: MatchRepository,
) : ViewModel() {
    val uiState: StateFlow<HomePlaceholderUiState> =
        matchRepository
            .observeAll()
            .map { matches ->
                HomePlaceholderUiState(
                    isLoading = false,
                    matches =
                        matches.map { match ->
                            MatchSummary(
                                id = match.id.toString(),
                                date = match.date,
                                format = match.format,
                                result = match.result,
                            )
                        },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomePlaceholderUiState())

    companion object {
        fun factory(dependencies: PicklelogDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { HomePlaceholderViewModel(dependencies.matchRepository) }
            }
    }
}
