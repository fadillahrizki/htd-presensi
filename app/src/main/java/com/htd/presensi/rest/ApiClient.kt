package com.htd.presensi.rest

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
//    const val BASE_URL = "http://103.151.71.42/api/"
//    const val BASE_URL = "https://api-presence.z-techno.com/api/"
    const val BASE_URL = "http://10.0.2.2:8000/api/"
    private var retrofit: Retrofit? = null
    val client: Retrofit?
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit
        }
}