package com.maxeydev.picklelog.data

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.maxeydev.picklelog.data.backup.DataStoreExportPromptStore
import com.maxeydev.picklelog.data.backup.RoomBackupRepository
import com.maxeydev.picklelog.data.billing.BillingClientWrapper
import com.maxeydev.picklelog.data.billing.BillingStartupCheck
import com.maxeydev.picklelog.data.billing.PlayEntitlementRepository
import com.maxeydev.picklelog.data.billing.PlayProStore
import com.maxeydev.picklelog.data.billing.PurchaseAcknowledger
import com.maxeydev.picklelog.data.db.DB_BACKUP_DIRECTORY
import com.maxeydev.picklelog.data.db.createPicklelogDatabase
import com.maxeydev.picklelog.data.erase.RoomLocalDataEraser
import com.maxeydev.picklelog.data.match.DataStoreLastUsedFormatStore
import com.maxeydev.picklelog.data.match.DataStoreMatchSortStore
import com.maxeydev.picklelog.data.match.RoomMatchRepository
import com.maxeydev.picklelog.data.match.createMatchPreferencesDataStore
import com.maxeydev.picklelog.data.person.RoomPersonRepository
import com.maxeydev.picklelog.data.photo.ImageCompressor
import com.maxeydev.picklelog.data.photo.PhotoStore
import com.maxeydev.picklelog.data.photo.QueuedPhotoImporter
import com.maxeydev.picklelog.data.profile.DataStoreEntitlementRepository
import com.maxeydev.picklelog.data.profile.DataStoreProfileRepository
import com.maxeydev.picklelog.data.profile.createUserProfileDataStore
import com.maxeydev.picklelog.data.reminder.DataStoreReminderStore
import com.maxeydev.picklelog.data.settings.DataStoreAppSettingsStore
import com.maxeydev.picklelog.data.settings.createAppSettingsDataStore
import com.maxeydev.picklelog.data.share.DataStoreCardFormatStore
import com.maxeydev.picklelog.data.streak.DataStoreStreakNoticeStore
import com.maxeydev.picklelog.domain.backup.BackupRepository
import com.maxeydev.picklelog.domain.billing.ProStore
import com.maxeydev.picklelog.domain.erase.LocalDataEraser
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchSortStore
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.photo.PhotoImportQueue
import com.maxeydev.picklelog.domain.profile.EntitlementRepository
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.reminder.ReminderStore
import com.maxeydev.picklelog.domain.settings.AppSettingsStore
import com.maxeydev.picklelog.domain.share.CardFormatStore
import com.maxeydev.picklelog.domain.streak.StreakNoticeStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock

const val EXPORT_CACHE_DIRECTORY = "exports"

const val ORPHAN_PHOTO_AGE_MILLIS = 24L * 60 * 60 * 1000

class DataLayer(
    private val roomMatchRepository: RoomMatchRepository,
    val personRepository: PersonRepository,
    val lastUsedFormatStore: LastUsedFormatStore,
    val matchSortStore: MatchSortStore,
    val photoStore: PhotoStore,
    val profileRepository: ProfileRepository,
    val entitlementRepository: EntitlementRepository,
    val proStore: ProStore,
    val billingStartupCheck: BillingStartupCheck,
    val photoImportQueue: PhotoImportQueue,
    val cardFormatStore: CardFormatStore,
    val backupRepository: BackupRepository,
    val streakNoticeStore: StreakNoticeStore,
    val reminderStore: ReminderStore,
    val appSettingsStore: AppSettingsStore,
    val localDataEraser: LocalDataEraser,
    private val ioDispatcher: CoroutineDispatcher,
) {
    val matchRepository: MatchRepository = roomMatchRepository

    suspend fun sweepOrphanPhotos(nowMillis: Long): Int {
        val referenced = roomMatchRepository.referencedPhotoPaths()
        return withContext(ioDispatcher) {
            photoStore.deleteOrphans(referenced, olderThanMillis = nowMillis - ORPHAN_PHOTO_AGE_MILLIS).size
        }
    }
}

suspend fun createDataLayer(
    context: Context,
    applicationScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    currentActivity: () -> Activity?,
): DataLayer {
    val appContext = context.applicationContext
    val database = createPicklelogDatabase(appContext, ioDispatcher)
    val preferences = createMatchPreferencesDataStore(appContext, applicationScope + ioDispatcher)
    val photoStore = PhotoStore(appContext.filesDir)
    val profileStore = createUserProfileDataStore(appContext, applicationScope + ioDispatcher)
    val profileRepository = DataStoreProfileRepository(profileStore)
    val appSettings =
        DataStoreAppSettingsStore(createAppSettingsDataStore(appContext, applicationScope + ioDispatcher))
    val matchRepository = RoomMatchRepository(database, photoStore, ioDispatcher)
    val billing = BillingClientWrapper(appContext)
    val entitlements =
        PlayEntitlementRepository(
            local = DataStoreEntitlementRepository(profileStore),
            gateway = billing,
            acknowledger = PurchaseAcknowledger(billing),
            clock = Clock.System,
        )
    val resolver = appContext.contentResolver
    val importer =
        QueuedPhotoImporter(
            scope = applicationScope,
            ioDispatcher = ioDispatcher,
            compressor = ImageCompressor(openSource = { uri -> resolver.openInputStream(Uri.parse(uri)) }),
            photoStore = photoStore,
            matchRepository = matchRepository,
            deleteTemporaryCapture = { uri -> resolver.delete(Uri.parse(uri), null, null) },
        )
    val backupRepository =
        RoomBackupRepository(
            database = database,
            photoStore = photoStore,
            entitlements = entitlements,
            promptStore = DataStoreExportPromptStore(preferences),
            exportDirectory = File(appContext.cacheDir, EXPORT_CACHE_DIRECTORY),
            openSource = { uri -> resolver.openInputStream(Uri.parse(uri)) },
            clock = Clock.System,
            ioDispatcher = ioDispatcher,
        )
    return DataLayer(
        roomMatchRepository = matchRepository,
        personRepository = RoomPersonRepository(database, ioDispatcher),
        lastUsedFormatStore = DataStoreLastUsedFormatStore(preferences, applicationScope),
        matchSortStore = DataStoreMatchSortStore(preferences),
        photoStore = photoStore,
        profileRepository = profileRepository,
        entitlementRepository = entitlements,
        proStore = PlayProStore(billing, entitlements) { currentActivity()?.let { billing.launchProPurchase(it) } },
        billingStartupCheck = BillingStartupCheck(entitlements, billing, applicationScope),
        photoImportQueue = importer,
        cardFormatStore = DataStoreCardFormatStore(preferences),
        backupRepository = backupRepository,
        streakNoticeStore = DataStoreStreakNoticeStore(preferences),
        reminderStore = DataStoreReminderStore(preferences),
        appSettingsStore = appSettings,
        localDataEraser =
            RoomLocalDataEraser(
                database = database,
                photoStore = photoStore,
                exportDirectory = File(appContext.cacheDir, EXPORT_CACHE_DIRECTORY),
                databaseBackupDirectory = File(appContext.noBackupFilesDir, DB_BACKUP_DIRECTORY),
                preferences = preferences,
                profileRepository = profileRepository,
                appSettings = appSettings,
                ioDispatcher = ioDispatcher,
            ),
        ioDispatcher = ioDispatcher,
    )
}
