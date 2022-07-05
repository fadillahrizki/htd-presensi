package com.htd.presensi.rest

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiInterface {
    @FormUrlEncoded
    @POST("auth/login")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("device_number") deviceNumber: String,
    ): Call<Any>

    @GET("employees/{id}")
    fun profile(@Header("authorization") token : String,@Path("id") id:String): Call<Any>

    @GET("employees/{id}/presences")
    fun getPresences(@Header("authorization") token : String,@Path("id") id: String,@Query("type") type:String,@Query("status") status:String, @Query("from_date") fromDate:String, @Query("to_date") toDate: String): Call<Any>

    @Multipart
    @POST("employees/{id}/presences")
    fun presences(
        @Header("authorization") token : String,
        @Path("id") id:String,
        @Part("type") type: RequestBody,
        @Part attachment : MultipartBody.Part,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("in_location") inLocation: RequestBody,
        @Part pic_url : MultipartBody.Part,
        @Part("worktime_item_id") worktimeItemId: RequestBody,
    ): Call<Any>


    @GET("employees/{id}/presences/{employee_precence_id}")
    fun getDetailPresence(@Header("authorization") token : String,@Path("id") id: String,@Path("employee_precence_id") employee_precence_id: String,): Call<Any>
}