package com.maxeydev.picklelog

import android.app.Application
import com.maxeydev.picklelog.data.createDataLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class PicklelogApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var container: Deferred<AppContainer>
        private set

    override fun onCreate() {
        super.onCreate()
        container =
            applicationScope.async {
                val dataLayer = createDataLayer(this@PicklelogApplication, applicationScope, Dispatchers.IO)
                applicationScope.launch { dataLayer.sweepOrphanPhotos(System.currentTimeMillis()) }
                AppContainer(this@PicklelogApplication, dataLayer)
            }
    }
}
