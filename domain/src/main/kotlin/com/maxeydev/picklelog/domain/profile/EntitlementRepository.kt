package com.maxeydev.picklelog.domain.profile

import com.maxeydev.picklelog.domain.entitlement.EntitlementSignal
import kotlinx.coroutines.flow.Flow

interface EntitlementRepository {
    fun observeEntitlement(): Flow<Entitlement>

    suspend fun record(signal: EntitlementSignal)
}
