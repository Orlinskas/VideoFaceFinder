package com.orlinskas.videofacefinder.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val builder = Room.databaseBuilder(context, AppDatabase::class.java, "database.db")
        // builder.addMigrations(Migration1To2())
        return builder.build()
    }

    class Migration1To2 : Migration(79, 80) {

        override fun migrate(database: SupportSQLiteDatabase) {

        }
    }

}
