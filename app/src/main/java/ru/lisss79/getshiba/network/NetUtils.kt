package ru.lisss79.getshiba.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ru.lisss79.getshiba.di.DaggerShibaComponent
import java.net.URL

object NetUtils {
    const val BASE_URL = "http://shibe.online/api/"
    fun getLinks(quantity: Int): List<String>? {
        val retrofit = DaggerShibaComponent.create().getRetrofit()
        val api = retrofit.create(ShibaApi::class.java)
        val response = runCatching { api.getPictures(quantity).execute() }
        return response.getOrNull()?.body()
    }

    fun getBitmap(url1: String): Bitmap? {
        val result = runCatching {
            URL(url1).openStream().use {
                BitmapFactory.decodeStream(it)
            }
        }
        return result.getOrDefault(null)
    }
}