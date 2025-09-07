package com.enigma.catat_arey.data.di

import android.content.Context
import com.enigma.catat_arey.data.preferences.DatastoreManager
import com.enigma.catat_arey.data.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatastoreModule {
    @Provides
    @Singleton
    fun provideDatastoreManager(userPreferences: UserPreferences): DatastoreManager {
        return DatastoreManager(userPreferences)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext ctx: Context): UserPreferences {
        return UserPreferences(ctx)
    }
}