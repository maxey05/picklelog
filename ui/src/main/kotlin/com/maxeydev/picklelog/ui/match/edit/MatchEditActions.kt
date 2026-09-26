package com.maxeydev.picklelog.ui.match.edit

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult

class MatchEditActions(
    val onFormatSelected: (MatchFormat) -> Unit,
    val onResultSelected: (MatchResult) -> Unit,
    val onDateSelected: (AppDate) -> Unit,
    val onStartTimeChanged: (AppTime?) -> Unit,
    val onEndTimeChanged: (AppTime?) -> Unit,
    val onPersonNameChanged: (PersonSlot, String) -> Unit,
    val onGameAdded: () -> Unit,
    val onGameRemoved: (Int) -> Unit,
    val onGameScoresChanged: (Int, String, String) -> Unit,
    val onLocationChanged: (String) -> Unit,
    val onPaddleChanged: (String) -> Unit,
    val onNotesChanged: (String) -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
)
