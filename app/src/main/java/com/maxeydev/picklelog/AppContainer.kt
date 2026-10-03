package com.maxeydev.picklelog

import android.content.Context
import androidx.annotation.MainThread
import androidx.work.WorkManager
import com.maxeydev.picklelog.data.DataLayer
import com.maxeydev.picklelog.data.photo.PhotoStore
import com.maxeydev.picklelog.data.reminder.ReminderScheduler
import com.maxeydev.picklelog.data.settings.DirectoryAppCache
import com.maxeydev.picklelog.domain.backup.BackupRepository
import com.maxeydev.picklelog.domain.billing.ProStore
import com.maxeydev.picklelog.domain.erase.EraseAllData
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchSortStore
import com.maxeydev.picklelog.domain.onboarding.Onboarding
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.photo.PhotoImportQueue
import com.maxeydev.picklelog.domain.profile.EntitlementRepository
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.reminder.ReminderStore
import com.maxeydev.picklelog.domain.reminder.StreakReminder
import com.maxeydev.picklelog.domain.settings.AppCache
import com.maxeydev.picklelog.domain.settings.AppSettingsStore
import com.maxeydev.picklelog.domain.share.CardFormatStore
import com.maxeydev.picklelog.domain.streak.InsuredStreakEngine
import com.maxeydev.picklelog.domain.streak.StreakNoticeStore
import com.maxeydev.picklelog.domain.streak.streakInsuranceStart
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.common.createCaptureUri
import com.maxeydev.picklelog.ui.notification.StreakReminderNotifier
import com.maxeydev.picklelog.ui.share.CardRenderer
import com.maxeydev.picklelog.ui.share.CardRendering
import com.maxeydev.picklelog.ui.share.WebViewWarmer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    override val entitlementRepository: EntitlementRepository = dataLayer.entitlementRepository
    override val proStore: ProStore = dataLayer.proStore
    override val photoImportQueue: PhotoImportQueue = dataLayer.photoImportQueue
    override val cardFormatStore: CardFormatStore = dataLayer.cardFormatStore
    override val backupRepository: BackupRepository = dataLayer.backupRepository
    override val streakNoticeStore: StreakNoticeStore = dataLayer.streakNoticeStore
    override val reminderStore: ReminderStore = dataLayer.reminderStore
    override val clock: Clock = Clock.System
    override val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    override val streakReminder: StreakReminder =
        StreakReminder(
            store = dataLayer.reminderStore,
            matchDates = {
                dataLayer.matchRepository
                    .observeStatLines(FilterState.NONE)
                    .first()
                    .map { it.date }
            },
            insuranceStart = {
                dataLayer.profileRepository
                    .observeProfile()
                    .first()
                    .entitlement
                    .streakInsuranceStart()
            },
            engine = InsuredStreakEngine(clock) { currentTimeZone() },
            scheduling = ReminderScheduler(WorkManager.getInstance(context), clock),
            notifier = StreakReminderNotifier(context),
            clock = clock,
            timeZone = { currentTimeZone() },
        )
    override val appSettingsStore: AppSettingsStore = dataLayer.appSettingsStore
    override val appCache: AppCache = DirectoryAppCache(context.cacheDir, Dispatchers.IO)
    override val onboarding: Onboarding =
        Onboarding(
            settings = dataLayer.appSettingsStore,
            profile = dataLayer.profileRepository,
            matchCount = { dataLayer.matchRepository.observeMatchCount().first() },
        )
    override val eraseAllData: EraseAllData = EraseAllData(streakReminder, dataLayer.localDataEraser)
    override val appVersionName: String =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
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
