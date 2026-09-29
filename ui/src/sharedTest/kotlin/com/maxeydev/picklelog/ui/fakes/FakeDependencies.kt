package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.ui.PicklelogDependencies
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.time.Clock

class FakeDependencies(
    override val matchRepository: FakeMatchRepository = FakeMatchRepository(),
    override val personRepository: FakePersonRepository = FakePersonRepository(matchSource = matchRepository),
    override val lastUsedFormatStore: FakeLastUsedFormatStore = FakeLastUsedFormatStore(),
    override val matchSortStore: FakeMatchSortStore = FakeMatchSortStore(),
    override val profileRepository: FakeProfileRepository = FakeProfileRepository(),
    override val entitlementRepository: FakeEntitlementRepository = FakeEntitlementRepository(),
    override val proStore: FakeProStore = FakeProStore(entitlementRepository),
    override val photoImportQueue: FakePhotoImportQueue = FakePhotoImportQueue(),
    override val cardRenderer: FakeCardRenderer = FakeCardRenderer(),
    override val cardFormatStore: FakeCardFormatStore = FakeCardFormatStore(),
    override val clock: Clock = Clock.System,
    override val defaultDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val photoRoot: File = File(System.getProperty("java.io.tmpdir"), "picklelog-fake-photos"),
) : PicklelogDependencies {
    override fun currentTimeZone(): TimeZone = timeZone

    override fun photoFile(relativePath: String): File = File(photoRoot, relativePath)

    override fun newCaptureUri(): String = "content://picklelog.test/camera/capture.jpg"
}
