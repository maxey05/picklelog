package com.maxeydev.picklelog.data.billing

const val PRO_PRODUCT_ID = "picklelog_pro"

enum class PlayPurchaseState {
    PURCHASED,
    PENDING,
    UNSPECIFIED,
}

data class PlayPurchase(
    val purchaseToken: String,
    val productIds: List<String>,
    val state: PlayPurchaseState,
    val isAcknowledged: Boolean,
    val purchasedAtMillis: Long? = null,
) {
    val isPro: Boolean
        get() = PRO_PRODUCT_ID in productIds
}
