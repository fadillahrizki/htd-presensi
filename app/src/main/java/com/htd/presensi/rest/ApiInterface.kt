package com.htd.presensi.rest

import retrofit2.Call
import retrofit2.http.*

interface ApiInterface {
    @FormUrlEncoded
    @POST("/index.php?r=api/business/findByToken")
    fun getBusiness(
        @Field("token") token: String,
        @Field("note") note: String
    ): Call<Any>

    @Headers("X-MINIPOS-APP: minipos")
    @FormUrlEncoded
    @POST("/index.php?r=api/auth/login")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<Any>

}