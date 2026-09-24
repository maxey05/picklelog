package com.maxeydev.picklelog.data.profile

import androidx.datastore.core.DataStore
import com.maxeydev.picklelog.domain.profile.Entitlement
import com.maxeydev.picklelog.domain.profile.EntitlementRepository
import com.maxeydev.picklelog.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreEntitlementRepository(
    private val dataStore: DataStore<UserProfile>,
) : EntitlementRepository {
    override fun observeEntitlement(): Flow<Entitlement> = dataStore.data.map { profile -> profile.entitlement }
}
