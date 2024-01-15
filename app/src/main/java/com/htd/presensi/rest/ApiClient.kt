package com.htd.presensi.rest

import android.util.Log
import com.htd.presensi.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {
//    const val BASE_URL = "http://103.151.71.42/"
    const val BASE_URL = "https://presensi.webisnis.id/"
//    const val BASE_URL = "https://api-presence.z-techno.com/"
//    const val BASE_URL = "http://10.0.2.2:8000/"
    private var retrofit: Retrofit? = null
    val client: Retrofit?
        get() {
            if (retrofit == null) {
                val httpClient = OkHttpClient.Builder()

                httpClient.addInterceptor { chain ->
                    val request: Request =
                        chain.request().newBuilder().addHeader("X-APP-VERSION", BuildConfig.VERSION_CODE.toString()).build()
                    chain.proceed(request)
                }


                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL+"api/")
                    .client(httpClient.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit
        }

}