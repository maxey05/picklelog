package com.maxeydev.picklelog.ui.settings

data class OpenSourceLibrary(
    val name: String,
    val license: String,
)

private const val APACHE_2 = "Apache License 2.0"

val OPEN_SOURCE_LIBRARIES: List<OpenSourceLibrary> =
    listOf(
        OpenSourceLibrary("Kotlin", APACHE_2),
        OpenSourceLibrary("kotlinx.coroutines", APACHE_2),
        OpenSourceLibrary("kotlinx.serialization", APACHE_2),
        OpenSourceLibrary("kotlinx-datetime", APACHE_2),
        OpenSourceLibrary("Jetpack Compose and Material 3", APACHE_2),
        OpenSourceLibrary("AndroidX Core, Activity, Lifecycle and Navigation", APACHE_2),
        OpenSourceLibrary("AndroidX Room", APACHE_2),
        OpenSourceLibrary("AndroidX DataStore", APACHE_2),
        OpenSourceLibrary("AndroidX WorkManager", APACHE_2),
        OpenSourceLibrary("Coil", APACHE_2),
        OpenSourceLibrary("Google Play Billing Library", "Android Software Development Kit License"),
    )
