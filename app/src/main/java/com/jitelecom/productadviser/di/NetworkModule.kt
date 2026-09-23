package com.jitelecom.productadviser.di

import com.jitelecom.productadviser.data.remote.UpdateManifestApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun updateApi(retrofit: Retrofit): UpdateManifestApi = retrofit.create(UpdateManifestApi::class.java)
}
