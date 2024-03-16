package ru.lisss79.getshiba.di

import dagger.Component
import retrofit2.Retrofit

@Component(modules = [NetworkModule::class])
interface ShibaComponent {
    fun getRetrofit(): Retrofit

}