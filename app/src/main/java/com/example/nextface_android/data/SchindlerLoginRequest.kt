package com.example.nextface_android.data

import com.google.gson.annotations.SerializedName

data class SchindlerLoginRequest(
    @SerializedName("username")
    var user: String,
    @SerializedName("password")
    var password: String
)
