package com.maxeydev.picklelog

import android.app.Application
import com.maxeydev.picklelog.data.createDataLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class PicklelogApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var container: Deferred<AppContainer>
        private set

    override fun onCreate() {
        super.onCreate()
        container =
            applicationScope.async {
                AppContainer(createDataLayer(this@PicklelogApplication, applicationScope, Dispatchers.IO))
            }
    }
}
