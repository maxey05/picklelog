package com.maxeydev.picklelog

import android.content.Context
import androidx.annotation.MainThread
import com.maxeydev.picklelog.data.DataLayer
import com.maxeydev.picklelog.data.photo.PhotoStore
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchSortStore
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.photo.PhotoImportQueue
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.common.createCaptureUri
import com.maxeydev.picklelog.ui.share.CardRenderer
import com.maxeydev.picklelog.ui.share.CardRendering
import com.maxeydev.picklelog.ui.share.WebViewWarmer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.time.Clock

class AppContainer(
    private val context: Context,
    dataLayer: DataLayer,
) : PicklelogDependencies {
    private val photoStore: PhotoStore = dataLayer.photoStore

    override val matchRepository: MatchRepository = dataLayer.matchRepository
    override val personRepository: PersonRepository = dataLayer.personRepository
    override val lastUsedFormatStore: LastUsedFormatStore = dataLayer.lastUsedFormatStore
    override val matchSortStore: MatchSortStore = dataLayer.matchSortStore
    override val profileRepository: ProfileRepository = dataLayer.profileRepository
    override val photoImportQueue: PhotoImportQueue = dataLayer.photoImportQueue
    override val clock: Clock = Clock.System
    override val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val cardWarmer = WebViewWarmer(context)
    override val cardRenderer: CardRendering = CardRenderer(cardWarmer)

    override fun currentTimeZone(): TimeZone = TimeZone.currentSystemDefault()

    override fun photoFile(relativePath: String): File = photoStore.resolve(relativePath)

    @MainThread
    fun warmCardRenderer() {
        cardWarmer.warm()
    }

    override fun newCaptureUri(): String = createCaptureUri(context, clock.now().toEpochMilliseconds())
}
