package com.example.nextface_android.data

import com.google.gson.annotations.SerializedName

data class FaceSearchRequest(
    @SerializedName("limit")
    var limit: Int,
    @SerializedName("image")
    var imageBase64: String,
    @SerializedName("serial")
    var serialNumber: String
)

