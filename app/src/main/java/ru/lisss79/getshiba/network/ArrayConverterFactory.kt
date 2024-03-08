package ru.lisss79.getshiba.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type


class ArrayConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        return ResponseConverter()
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        return RequestConverter()
    }

    companion object {
        fun create() = ArrayConverterFactory()
    }

    class ResponseConverter : Converter<ResponseBody, List<String>> {
        override fun convert(value: ResponseBody): List<String> {
            val array = JSONArray(value.string())
            return List(array.length()) { array[it].toString() }
        }
    }
    class RequestConverter : Converter<String, RequestBody> {
        override fun convert(value: String): RequestBody? {
            return RequestBody.create(MediaType.parse("text/plain"), value)
        }
    }

}