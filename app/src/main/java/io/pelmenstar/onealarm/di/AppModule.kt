package io.pelmenstar.onealarm.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.pelmenstar.onealarm.appPreferences
import io.pelmenstar.onealarm.audio.AlarmKlaxon
import io.pelmenstar.onealarm.audio.DefaultAlarmKlaxon
import io.pelmenstar.onealarm.data.AndroidInternalAlarmsManager
import io.pelmenstar.onealarm.data.AppDatabase
import io.pelmenstar.onealarm.data.InternalAlarmsManager

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "database").build()
    }

    @Provides
    fun provideAlarmsManager(@ApplicationContext context: Context): InternalAlarmsManager {
        return AndroidInternalAlarmsManager(context)
    }

    @Provides
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.appPreferences
    }

    @Provides
    fun provideAlarmKlaxon(@ApplicationContext context: Context): AlarmKlaxon {
        return DefaultAlarmKlaxon(context)
    }
}