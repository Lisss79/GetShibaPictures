package ru.lisss79.getshiba.di

import dagger.Module
import dagger.Provides
import retrofit2.Converter
import retrofit2.Retrofit
import ru.lisss79.getshiba.network.ArrayConverterFactory
import ru.lisss79.getshiba.network.NetUtils.BASE_URL

@Module
object NetworkModule {

    @Provides
    fun provideRetrofit(converterFactory: Converter.Factory): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(converterFactory)
            .build()
    }

    @Provides
    fun provideFactory(): Converter.Factory {
        return ArrayConverterFactory.create()
    }

}