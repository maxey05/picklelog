@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.person.normalizePersonName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FakePersonRepository(
    initial: List<Person> = emptyList(),
) : PersonRepository {
    private val people = MutableStateFlow(initial)

    val current: List<Person>
        get() = people.value

    override fun observeAll(): Flow<List<Person>> = people

    override suspend fun findOrCreatePerson(displayName: String): Person {
        val normalized = normalizePersonName(displayName)
        require(normalized.isNotEmpty()) { "A person needs a display name with at least one non-whitespace character." }
        people.value.firstOrNull { normalizePersonName(it.displayName) == normalized }?.let { return it }
        val created = Person(Uuid.random(), displayName.trim(), AppInstant.fromEpochMilliseconds(0))
        people.update { it + created }
        return created
    }
}
