package com.maxeydev.picklelog.ui

import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.person.PersonRepository
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

interface PicklelogDependencies {
    val matchRepository: MatchRepository
    val personRepository: PersonRepository
    val lastUsedFormatStore: LastUsedFormatStore
    val clock: Clock

    fun currentTimeZone(): TimeZone
}
