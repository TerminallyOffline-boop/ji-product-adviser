package com.jitelecom.productadviser

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jitelecom.productadviser.data.seed.DemoDataSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JIProductAdviserApplication : Application(), Configuration.Provider {
    @Inject lateinit var seeder: DemoDataSeeder
    @Inject lateinit var workerFactory: HiltWorkerFactory
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                seeder.seedIfEmpty()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Bundled data refresh failed; keeping the app process available.", error)
            }
        }
    }

    private companion object {
        const val TAG = "JIProductAdviser"
    }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
