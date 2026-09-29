package com.maxeydev.picklelog.domain.entitlement

import com.maxeydev.picklelog.domain.datetime.AppInstant

sealed interface EntitlementSignal {
    data class PurchaseVerified(
        val purchaseToken: String,
        val verifiedAt: AppInstant,
    ) : EntitlementSignal

    data class RefundConfirmed(
        val purchaseToken: String,
    ) : EntitlementSignal

    data object NoPurchaseFound : EntitlementSignal

    data object CheckFailed : EntitlementSignal

    data object CheckUnavailable : EntitlementSignal
}
