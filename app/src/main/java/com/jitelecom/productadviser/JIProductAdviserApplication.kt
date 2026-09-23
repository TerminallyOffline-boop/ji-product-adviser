package com.jitelecom.productadviser

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jitelecom.productadviser.data.seed.DemoDataSeeder
import dagger.hilt.android.HiltAndroidApp
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
    override fun onCreate() { super.onCreate(); applicationScope.launch { seeder.seedIfEmpty() } }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
