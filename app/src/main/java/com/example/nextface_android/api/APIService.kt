package com.example.nextface_android.api

import com.example.nextface_android.Constants
import com.example.nextface_android.data.LoginRequest
import com.example.nextface_android.data.LoginResponse
import com.example.nextface_android.data.SchindlerLoginRequest
import com.example.nextface_android.data.SchindlerLoginResponse
import retrofit2.Call
import retrofit2.http.*

interface APIService {
    /*
       POST METHOD
    */
    @POST(Constants.API_FS_LOGIN_URL)
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}