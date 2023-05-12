package com.example.nextface_android.data

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("access_token")
    var authToken: String
)

