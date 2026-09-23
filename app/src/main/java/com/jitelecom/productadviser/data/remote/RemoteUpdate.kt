package com.jitelecom.productadviser.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.jitelecom.productadviser.BuildConfig
import com.jitelecom.productadviser.data.importexport.ImportExportManager
import com.jitelecom.productadviser.data.local.MetadataDao
import com.jitelecom.productadviser.data.preferences.AppPreferences
import com.squareup.moshi.JsonClass
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.http.GET
import retrofit2.http.Url

@JsonClass(generateAdapter = false)
data class UpdateManifest(
    val databaseVersion: String,
    val minimumAppVersion: String,
    val downloadUrl: String,
    val checksum: String,
    val releaseNotes: String = "",
    val publishedAt: String
)

interface UpdateManifestApi { @GET suspend fun getManifest(@Url url: String): UpdateManifest }

@HiltWorker
class RemoteUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val preferences: AppPreferences,
    private val client: OkHttpClient,
    private val api: UpdateManifestApi,
    private val metadata: MetadataDao,
    private val importExport: ImportExportManager
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val url = preferences.settings.first().remoteDatabaseUrl
        if (url.isBlank()) return failure("Set a remote manifest URL first.")
        return try {
            val manifest = api.getManifest(url)
            if (compareVersion(manifest.minimumAppVersion, BuildConfig.VERSION_NAME) > 0) return failure("Update requires app ${manifest.minimumAppVersion}.")
            val current = metadata.getValue("databaseVersion")
            if (current != null && compareVersion(manifest.databaseVersion, current) <= 0) {
                return Result.success(Data.Builder().putString("message", "Database is already up to date ($current).").build())
            }
            val response = client.newCall(Request.Builder().url(manifest.downloadUrl).build()).execute()
            val bytes = response.use { if (!it.isSuccessful) error("Download failed with HTTP ${it.code}"); it.body?.bytes() ?: error("Empty update file") }
            val name = manifest.downloadUrl.substringAfterLast('/').substringBefore('?').ifBlank { "database.json" }
            val imported = importExport.importPackage(bytes, name, manifest.checksum)
            if (imported.success) Result.success(Data.Builder().putString("message", imported.message).build()) else failure(imported.message)
        } catch (error: Exception) { if (runAttemptCount < 2) Result.retry() else failure(error.message ?: "Update failed.") }
    }
    private fun failure(message: String) = Result.failure(Data.Builder().putString("message", message).build())
    private fun compareVersion(a: String, b: String): Int { val x=a.split('.').mapNotNull(String::toIntOrNull); val y=b.split('.').mapNotNull(String::toIntOrNull); for(i in 0 until maxOf(x.size,y.size)){ val d=x.getOrElse(i){0}-y.getOrElse(i){0}; if(d!=0)return d }; return 0 }
}
