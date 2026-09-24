package com.maxeydev.picklelog.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>

    suspend fun updateDisplayName(displayName: String)
}
