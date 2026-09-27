package com.maxeydev.picklelog.data.match

internal const val OPPONENTS_OF_OUTER_MATCH =
    "FROM match_person mp INNER JOIN person p ON p.id = mp.person_id " +
        "WHERE mp.match_id = m.id AND mp.role = '${MatchPersonEntity.ROLE_OPPONENT}' ORDER BY mp.slot"

internal const val FIRST_OPPONENT_NAME = "(SELECT p.display_name $OPPONENTS_OF_OUTER_MATCH LIMIT 1)"

internal const val SECOND_OPPONENT_NAME = "(SELECT p.display_name $OPPONENTS_OF_OUTER_MATCH LIMIT 1 OFFSET 1)"

internal const val FIRST_OPPONENT_SORT_KEY = "(SELECT p.normalized_name $OPPONENTS_OF_OUTER_MATCH LIMIT 1)"

internal const val PRIMARY_PHOTO_PATH =
    "(SELECT ph.relative_path FROM photo ph WHERE ph.match_id = m.id ORDER BY ph.sort_index LIMIT 1)"

internal const val MATCH_LIST_SELECT =
    "SELECT m.id AS id, m.date AS date, m.format AS format, m.result AS result, " +
        "$FIRST_OPPONENT_NAME AS first_opponent, " +
        "$SECOND_OPPONENT_NAME AS second_opponent, " +
        "$PRIMARY_PHOTO_PATH AS primary_photo_path " +
        "FROM `match` m"

internal const val NEWEST_FIRST = "m.date DESC, m.created_at DESC, m.id ASC"

internal const val OLDEST_FIRST = "m.date ASC, m.created_at ASC, m.id ASC"

internal const val WINS_BEFORE_LOSSES = "CASE m.result WHEN 'WIN' THEN 0 ELSE 1 END"

internal const val LOSSES_BEFORE_WINS = "CASE m.result WHEN 'LOSS' THEN 0 ELSE 1 END"

internal const val NO_OPPONENT_LAST_THEN_A_TO_Z =
    "$FIRST_OPPONENT_SORT_KEY IS NULL, $FIRST_OPPONENT_SORT_KEY ASC"

internal const val PAGE_LIMIT = "LIMIT :limit"
