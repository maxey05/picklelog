@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.domain.person

import com.maxeydev.picklelog.domain.datetime.AppInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Person(
    val id: Uuid,
    val displayName: String,
    val createdAt: AppInstant,
)
