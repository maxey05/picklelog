package com.maxeydev.picklelog

import android.app.Application
import com.maxeydev.picklelog.data.billing.ForegroundActivity
import com.maxeydev.picklelog.data.createDataLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class PicklelogApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val foregroundActivity = ForegroundActivity()

    lateinit var container: Deferred<AppContainer>
        private set

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(foregroundActivity)
        container =
            applicationScope.async {
                val dataLayer =
                    createDataLayer(this@PicklelogApplication, applicationScope, Dispatchers.IO) {
                        foregroundActivity.current
                    }
                applicationScope.launch { dataLayer.sweepOrphanPhotos(System.currentTimeMillis()) }
                dataLayer.billingStartupCheck.start()
                AppContainer(this@PicklelogApplication, dataLayer)
            }
    }
}
