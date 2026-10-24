package com.ascolith.textseek.core.database.di

import android.content.Context
import androidx.room.Room
import com.ascolith.textseek.core.database.TextSeekDatabase
import com.ascolith.textseek.core.database.dao.SearchGroupDao
import com.ascolith.textseek.core.database.dao.SearchSettingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    fun provideSearchGroupDao(textSeekDatabase: TextSeekDatabase): SearchGroupDao {
        return textSeekDatabase.searchGroupDao()
    }

    @Provides
    fun provideSearchSettingDao(textSeekDatabase: TextSeekDatabase): SearchSettingDao {
        return textSeekDatabase.searchSettingDao()
    }

    @Provides
    @Singleton
    fun provideTextSeekDatabase(@ApplicationContext appContext: Context): TextSeekDatabase {
        return Room.databaseBuilder(
            appContext,
            TextSeekDatabase::class.java,
            "TextSeekDatabase"
        )
            .build()
    }
}
