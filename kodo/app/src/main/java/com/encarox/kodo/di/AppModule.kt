package com.encarox.kodo.di

import android.content.Context
import androidx.room.Room
import com.encarox.kodo.data.db.CodeDao
import com.encarox.kodo.data.db.KodoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Unqualified Context binding so plain-Context constructors resolve. */
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KodoDatabase =
        Room.databaseBuilder(context, KodoDatabase::class.java, KodoDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCodeDao(db: KodoDatabase): CodeDao = db.codeDao()
}
