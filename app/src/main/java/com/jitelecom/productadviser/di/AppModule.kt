package com.jitelecom.productadviser.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.jitelecom.productadviser.data.local.*
import com.jitelecom.productadviser.data.repository.*
import com.jitelecom.productadviser.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ji_product_adviser.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    @Provides fun productDao(db: AppDatabase) = db.productDao()
    @Provides fun hardwareDao(db: AppDatabase) = db.hardwareDao()
    @Provides fun softwareDao(db: AppDatabase) = db.softwareDao()
    @Provides fun requirementDao(db: AppDatabase) = db.requirementDao()
    @Provides fun metadataDao(db: AppDatabase) = db.metadataDao()
    @Provides fun analyticsDao(db: AppDatabase) = db.analyticsDao()
    @Provides @Singleton fun workManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
    @Provides @Singleton fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }).build()
    @Provides @Singleton fun moshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    @Provides @Singleton fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("https://localhost.invalid/").client(client).addConverterFactory(MoshiConverterFactory.create(moshi)).build()
}

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun products(impl: ProductRepositoryImpl): ProductRepository
    @Binds @Singleton abstract fun catalog(impl: CatalogRepositoryImpl): CatalogRepository
}
