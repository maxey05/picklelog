package com.maxeydev.picklelog.domain.person

import kotlinx.coroutines.flow.Flow

interface PersonRepository {
    fun observeAll(): Flow<List<Person>>

    suspend fun findOrCreatePerson(displayName: String): Person
}
