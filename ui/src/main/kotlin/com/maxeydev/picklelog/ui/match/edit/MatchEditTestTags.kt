package com.maxeydev.picklelog.ui.match.edit

object MatchEditTestTags {
    const val SAVE = "match_edit_save"
    const val RESULT_WIN = "match_edit_result_win"
    const val RESULT_LOSS = "match_edit_result_loss"
    const val FORMAT_SINGLES = "match_edit_format_singles"
    const val FORMAT_DOUBLES = "match_edit_format_doubles"
    const val ADD_GAME = "match_edit_add_game"

    fun personSlot(slot: PersonSlot): String = "match_edit_person_${slot.name.lowercase()}"
}
