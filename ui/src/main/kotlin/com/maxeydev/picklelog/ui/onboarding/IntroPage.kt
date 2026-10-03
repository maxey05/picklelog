package com.maxeydev.picklelog.ui.onboarding

import androidx.annotation.StringRes
import com.maxeydev.picklelog.ui.R

enum class IntroPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val illustrationDescription: Int,
    val viewportHeight: Float,
) {
    LOGGING(
        title = R.string.onboarding_logging_title,
        body = R.string.onboarding_logging_body,
        illustrationDescription = R.string.intro_art_logging_description,
        viewportHeight = 440f,
    ),
    STATS(
        title = R.string.onboarding_stats_title,
        body = R.string.onboarding_stats_body,
        illustrationDescription = R.string.intro_art_stats_description,
        viewportHeight = 440f,
    ),
    SHARE(
        title = R.string.onboarding_share_title,
        body = R.string.onboarding_share_body,
        illustrationDescription = R.string.intro_art_share_description,
        viewportHeight = 440f,
    ),
    PRIVACY(
        title = R.string.onboarding_privacy_title,
        body = R.string.onboarding_privacy_body,
        illustrationDescription = R.string.intro_art_privacy_description,
        viewportHeight = 330f,
    ),
}
