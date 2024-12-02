package com.enigma.catat_arey.data.di

import com.enigma.catat_arey.data.network.AreyApiConfig
import com.enigma.catat_arey.data.network.AreyApiService
import com.enigma.catat_arey.data.network.AreyTokenProvider
import com.enigma.catat_arey.util.AreyConsts
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiKey

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @BaseUrl
    @Provides
    @Singleton
    fun provideBaseUrl(): String = AreyConsts.getBaseApiUrl()

    @ApiKey
    @Provides
    @Singleton
    fun provideApiKey(): String = AreyConsts.getBaseApiKey()

    @Provides
    @Singleton
    fun provideTokenProvider(): AreyTokenProvider = AreyTokenProvider()

    @Provides
    @DefaultApi
    fun provideDefaultInterceptor(@ApiKey apiKey: String): Interceptor {
        return AreyApiConfig.DefaultInterceptor(apiKey)
    }

    @Provides
    @AuthenticatedApi
    fun provideAuthenticatedInterceptor(
        @ApiKey apiKey: String,
        tokenProvider: AreyTokenProvider
    ): Interceptor {
        return AreyApiConfig.AuthenticatedInterceptor(apiKey, tokenProvider)
    }

    @Provides
    @DefaultApi
    fun provideDefaultOkHttpClient(@DefaultApi defaultInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(defaultInterceptor)
            .build()
    }

    @Provides
    @AuthenticatedApi
    fun provideAuthenticatedOkHttpClient(@AuthenticatedApi authenticatedInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authenticatedInterceptor)
            .build()
    }

    @Provides
    @DefaultApi
    fun provideDefaultRetrofit(
        @BaseUrl baseUrl: String,
        @DefaultApi defaultClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(defaultClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @AuthenticatedApi
    fun provideAuthenticatedRetrofit(
        @BaseUrl baseUrl: String,
        @AuthenticatedApi authenticatedClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @DefaultApi
    @Singleton
    fun provideDefaultApiService(@DefaultApi retrofit: Retrofit): AreyApiService {
        return retrofit.create(AreyApiService::class.java)
    }

    @Provides
    @AuthenticatedApi
    @Singleton
    fun provideAuthenticatedApiService(@AuthenticatedApi retrofit: Retrofit): AreyApiService {
        return retrofit.create(AreyApiService::class.java)
    }
}