package com.maxeydev.picklelog.domain.profile

import com.maxeydev.picklelog.domain.datetime.AppInstant

data class Entitlement(
    val isPro: Boolean,
    val purchaseToken: String? = null,
    val lastVerifiedAt: AppInstant? = null,
)
