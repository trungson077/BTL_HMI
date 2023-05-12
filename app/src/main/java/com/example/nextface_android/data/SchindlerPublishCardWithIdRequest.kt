package com.example.nextface_android.data

import com.google.gson.annotations.SerializedName

data class SchindlerPublishCardWithIdRequest(
    @SerializedName("gateway_id")
    var gatewayId : String,
    @SerializedName("jdata")
    var jdata : JData
)

data class JData(
    @SerializedName("command")
    var command : String,
    @SerializedName("device_id")
    var deviceId : String,
    @SerializedName("parameter")
    var parameter : Parameter
)

data class Parameter(
    @SerializedName("TerminalID")
    var terminalID : String,
    @SerializedName("Destination")
    var destination : String,
    @SerializedName("personID")
    var personID : String,
    @SerializedName("Other")
    var other : String
)