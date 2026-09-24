package com.maxeydev.picklelog.domain.profile

import kotlinx.coroutines.flow.Flow

interface EntitlementRepository {
    fun observeEntitlement(): Flow<Entitlement>
}
