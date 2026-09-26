package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.ui.PicklelogDependencies
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class FakeDependencies(
    override val matchRepository: FakeMatchRepository = FakeMatchRepository(),
    override val personRepository: FakePersonRepository = FakePersonRepository(),
    override val lastUsedFormatStore: FakeLastUsedFormatStore = FakeLastUsedFormatStore(),
    override val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : PicklelogDependencies {
    override fun currentTimeZone(): TimeZone = timeZone
}
