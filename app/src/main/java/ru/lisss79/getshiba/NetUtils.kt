package ru.lisss79.getshiba

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import retrofit2.Retrofit
import java.io.FileInputStream
import java.net.URL

object NetUtils {
    private const val BASE_URL = "http://shibe.online/api/"
    fun getLinks(quantity: Int): List<String>? {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ArrayConverterFactory.create())
            .build()
        val api = retrofit.create(ShibaApi::class.java)
        val response = api.getPictures(quantity).execute()
        return response.body()
    }

    fun getBitmap(url1: String?): Bitmap {
        URL(url1).openStream().use {
            return BitmapFactory.decodeStream(it)
        }
    }
}