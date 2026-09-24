package com.maxeydev.picklelog.domain.profile

import com.maxeydev.picklelog.domain.datetime.AppInstant

data class UserProfile(
    val displayName: String,
    val createdAt: AppInstant,
    val entitlement: Entitlement,
)
