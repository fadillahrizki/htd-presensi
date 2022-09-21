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

    @FormUrlEncoded
    @POST("auth/forgot-password")
    fun forgotPassword(
        @Field("email") email: String
    ): Call<Any>

    @FormUrlEncoded
    @POST("auth/change-password")
    fun changePassword(
        @Header("authorization") token : String,
        @Field("password") password: String,
        @Field("password_confirmation") password_confirmation: String,
    ): Call<Any>

    @FormUrlEncoded
    @POST("auth/change-email")
    fun changeEmail(
        @Header("authorization") token : String,
        @Field("email") Email: String
    ): Call<Any>

    @GET("employees/{id}")
    fun profile(@Header("authorization") token : String,@Path("id") id:String): Call<Any>

    @GET("employees/detail-by-nip/{nip}")
    fun profileByNip(@Header("authorization") token : String,@Path("nip") nip:String): Call<Any>

    @GET("employees/{id}/presences")
    fun getPresences(@Header("authorization") token : String,@Path("id") id: String,@Query("type") type:String,@Query("status") status:String, @Query("date_from") fromDate:String, @Query("date_to") toDate: String): Call<Any>

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

    @Multipart
    @POST("employees/{id}/presences/{employee_presence_id}/upload-attachment")
    fun uploadAttachment(
        @Header("authorization") token : String,
        @Path("id") id:String,
        @Path("employee_presence_id") employee_presence_id:String,
        @Part attachment : MultipartBody.Part,
    ): Call<Any>

    @GET("employees/{id}/presences/{employee_precence_id}")
    fun getDetailPresence(@Header("authorization") token : String,@Path("id") id: String,@Path("employee_precence_id") employee_precence_id: String,): Call<Any>

    @GET("times")
    fun getTimes():Call<Any>

    @GET("employees/{id}/presences/check_if_exists/{worktime_item_id}")
    fun checkIfExists(@Header("authorization") token : String,@Path("id") id: String,@Path("worktime_item_id") worktime_item_id: String,): Call<Any>

    @GET("employees/reports/{workunit_id}")
    fun reports(@Header("authorization") token : String,@Path("workunit_id") workunit_id: String,@Query("date_start") dateStart:String, @Query("date_end") dateEnd: String,@Query("keyword") name:String): Call<Any>

    @GET("employees/report-details/{workunit_id}")
    fun reportDetails(@Header("authorization") token : String,@Path("workunit_id") workunit_id: String,@Query("date_start") dateStart:String, @Query("date_end") dateEnd: String,@Query("keyword") name:String): Call<Any>

}