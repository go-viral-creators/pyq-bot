package com.kaafisir.auramic.di

import com.kaafisir.auramic.data.settings.DataStoreSettingsRepository
import com.kaafisir.auramic.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindAiEnhancer(impl: com.kaafisir.auramic.audio.DfnEnhancer): com.kaafisir.auramic.audio.AiEnhancer

    @Binds
    @Singleton
    abstract fun bindPro(impl: com.kaafisir.auramic.data.pro.PlayProAccess): com.kaafisir.auramic.data.pro.ProAccess

    @Binds
    @Singleton
    abstract fun bindSettings(impl: DataStoreSettingsRepository): SettingsRepository
}
