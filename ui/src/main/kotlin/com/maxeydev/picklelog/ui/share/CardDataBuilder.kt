package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.share.CardFormat
import com.maxeydev.picklelog.domain.share.CardLayout
import com.maxeydev.picklelog.domain.streak.StreakResult

fun buildCardData(
    match: Match,
    displayName: String,
    streak: StreakResult,
    photoDataUri: String?,
    labels: CardLabels,
    format: CardFormat = CardFormat.DEFAULT,
): CardData =
    CardData(
        brand = labels.brand,
        displayName = displayName.trim(),
        meta = labels.meta(match.format, match.date),
        result = labels.result(match.result),
        isWin = match.result == MatchResult.WIN,
        opponents = labels.opponents(match.opponents.map { it.displayName }),
        partner = match.partner?.let { labels.partner(it.displayName) },
        score = labels.score(match.games.sortedBy { it.gameNumber }),
        location = match.location?.trim()?.takeIf { it.isNotEmpty() },
        streak = labels.streak(streak.current),
        photo = photoDataUri?.takeIf { format.layoutFor(hasPhoto = true) == CardLayout.PHOTO },
        ratio = format.ratio,
        theme = format.theme,
    )
