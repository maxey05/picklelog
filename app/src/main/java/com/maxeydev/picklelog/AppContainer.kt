package com.maxeydev.picklelog

import com.maxeydev.picklelog.data.DataLayer
import com.maxeydev.picklelog.data.photo.PhotoFileStore
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchSortStore
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.ui.PicklelogDependencies
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.time.Clock

class AppContainer(
    dataLayer: DataLayer,
) : PicklelogDependencies {
    private val photoFileStore: PhotoFileStore = dataLayer.photoFileStore

    override val matchRepository: MatchRepository = dataLayer.matchRepository
    override val personRepository: PersonRepository = dataLayer.personRepository
    override val lastUsedFormatStore: LastUsedFormatStore = dataLayer.lastUsedFormatStore
    override val matchSortStore: MatchSortStore = dataLayer.matchSortStore
    override val clock: Clock = Clock.System
    override val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun currentTimeZone(): TimeZone = TimeZone.currentSystemDefault()

    override fun photoFile(relativePath: String): File = photoFileStore.resolve(relativePath)
}
