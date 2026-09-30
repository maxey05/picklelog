package com.maxeydev.picklelog

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import androidx.work.WorkManager
import com.maxeydev.picklelog.data.DataLayer
import com.maxeydev.picklelog.data.backup.ExportPromptScheduler
import com.maxeydev.picklelog.data.backup.ExportPromptWorkerFactory
import com.maxeydev.picklelog.data.billing.ForegroundActivity
import com.maxeydev.picklelog.data.createDataLayer
import com.maxeydev.picklelog.data.reminder.StreakReminderWorkerFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val LOG_TAG = "Picklelog"

class PicklelogApplication :
    Application(),
    Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val foregroundActivity = ForegroundActivity()

    lateinit var container: Deferred<AppContainer>
        private set

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(
                    DelegatingWorkerFactory().apply {
                        addFactory(ExportPromptWorkerFactory { container.await().backupRepository })
                        addFactory(StreakReminderWorkerFactory { container.await().streakReminder })
                    },
                ).build()

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
                scheduleExportPrompts(dataLayer)
                AppContainer(this@PicklelogApplication, dataLayer).also { rearmStreakReminder(it) }
            }
    }

    private fun rearmStreakReminder(appContainer: AppContainer) {
        applicationScope.launch {
            try {
                appContainer.streakReminder.ensureArmed()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                Log.w(LOG_TAG, "Streak reminder could not be re-armed: ${failure.javaClass.simpleName}")
            }
        }
    }

    private fun scheduleExportPrompts(dataLayer: DataLayer) {
        try {
            val scheduler = ExportPromptScheduler(WorkManager.getInstance(this))
            scheduler.schedulePeriodicCheck()
            applicationScope.launch {
                dataLayer.matchRepository
                    .observeMatchCount()
                    .distinctUntilChanged()
                    .collect { scheduler.checkNow() }
            }
        } catch (notReady: IllegalStateException) {
            Log.w(LOG_TAG, "Export reminders could not be scheduled.", notReady)
        }
    }
}
