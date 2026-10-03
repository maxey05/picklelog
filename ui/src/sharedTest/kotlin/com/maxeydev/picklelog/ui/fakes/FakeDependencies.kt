package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.erase.EraseAllData
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.onboarding.Onboarding
import com.maxeydev.picklelog.domain.reminder.StreakReminder
import com.maxeydev.picklelog.domain.streak.InsuredStreakEngine
import com.maxeydev.picklelog.domain.streak.streakInsuranceStart
import com.maxeydev.picklelog.ui.PicklelogDependencies
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    override val backupRepository: FakeBackupRepository = FakeBackupRepository(),
    override val streakNoticeStore: FakeStreakNoticeStore = FakeStreakNoticeStore(),
    override val reminderStore: FakeReminderStore = FakeReminderStore(),
    val reminderScheduling: FakeReminderScheduling = FakeReminderScheduling(),
    val reminderNotifier: FakeReminderNotifier = FakeReminderNotifier(),
    override val clock: Clock = Clock.System,
    override val defaultDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    override val streakReminder: StreakReminder =
        StreakReminder(
            store = reminderStore,
            matchDates = { matchRepository.observeStatLines(FilterState.NONE).first().map { it.date } },
            insuranceStart = { profileRepository.observeProfile().first().entitlement.streakInsuranceStart() },
            engine = InsuredStreakEngine(clock) { timeZone },
            scheduling = reminderScheduling,
            notifier = reminderNotifier,
            clock = clock,
            timeZone = { timeZone },
        ),
    override val appSettingsStore: FakeAppSettingsStore = FakeAppSettingsStore(),
    override val onboarding: Onboarding =
        Onboarding(appSettingsStore, profileRepository) { matchRepository.observeMatchCount().first() },
    val localDataEraser: FakeLocalDataEraser = FakeLocalDataEraser(),
    override val eraseAllData: EraseAllData = EraseAllData(streakReminder, localDataEraser),
    override val appVersionName: String = "1.0.0-test",
    private val photoRoot: File = File(System.getProperty("java.io.tmpdir"), "picklelog-fake-photos"),
) : PicklelogDependencies {
    override fun currentTimeZone(): TimeZone = timeZone

    override fun photoFile(relativePath: String): File = File(photoRoot, relativePath)

    override fun newCaptureUri(): String = "content://picklelog.test/camera/capture.jpg"
}
