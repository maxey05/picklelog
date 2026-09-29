package com.maxeydev.picklelog.domain.billing

interface ProStore {
    suspend fun loadPrice(): PriceResult

    suspend fun purchase(): PurchaseOutcome

    suspend fun restore(): RestoreOutcome
}
