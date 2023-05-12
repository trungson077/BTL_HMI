package com.example.nextface_android.data

import com.google.gson.annotations.SerializedName

data class SchindlerLoginResponse(
    @SerializedName("token")
    var authToken: String
)
