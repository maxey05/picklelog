package com.maxeydev.picklelog.domain.person

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant

data class PersonUsage(
    val person: Person,
    val lastPlayedOn: AppDate,
    val lastLoggedAt: AppInstant,
)
