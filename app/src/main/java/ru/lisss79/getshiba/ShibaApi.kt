package ru.lisss79.getshiba

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ShibaApi {
    @GET("shibes")
    fun getPictures(@Query("count") quantity: Int): Call<List<String>>
}