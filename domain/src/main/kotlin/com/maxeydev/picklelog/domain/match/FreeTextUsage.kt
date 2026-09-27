package com.maxeydev.picklelog.domain.match

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant

data class FreeTextUsage(
    val value: String,
    val lastPlayedOn: AppDate,
    val lastLoggedAt: AppInstant,
)
