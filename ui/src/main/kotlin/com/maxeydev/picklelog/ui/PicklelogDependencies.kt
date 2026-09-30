package com.maxeydev.picklelog.ui

import com.maxeydev.picklelog.domain.backup.BackupRepository
import com.maxeydev.picklelog.domain.billing.ProStore
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchSortStore
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.photo.PhotoImportQueue
import com.maxeydev.picklelog.domain.profile.EntitlementRepository
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.share.CardFormatStore
import com.maxeydev.picklelog.ui.share.CardRendering
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.time.Clock

interface PicklelogDependencies {
    val matchRepository: MatchRepository
    val personRepository: PersonRepository
    val lastUsedFormatStore: LastUsedFormatStore
    val matchSortStore: MatchSortStore
    val profileRepository: ProfileRepository
    val entitlementRepository: EntitlementRepository
    val proStore: ProStore
    val photoImportQueue: PhotoImportQueue
    val cardRenderer: CardRendering
    val cardFormatStore: CardFormatStore
    val backupRepository: BackupRepository
    val clock: Clock
    val defaultDispatcher: CoroutineDispatcher
    val ioDispatcher: CoroutineDispatcher

    fun currentTimeZone(): TimeZone

    fun photoFile(relativePath: String): File

    fun newCaptureUri(): String
}
