package com.maxeydev.picklelog

import com.maxeydev.picklelog.data.DataLayer
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.ui.PicklelogDependencies
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class AppContainer(
    dataLayer: DataLayer,
) : PicklelogDependencies {
    override val matchRepository: MatchRepository = dataLayer.matchRepository
    override val personRepository: PersonRepository = dataLayer.personRepository
    override val lastUsedFormatStore: LastUsedFormatStore = dataLayer.lastUsedFormatStore
    override val clock: Clock = Clock.System

    override fun currentTimeZone(): TimeZone = TimeZone.currentSystemDefault()
}
