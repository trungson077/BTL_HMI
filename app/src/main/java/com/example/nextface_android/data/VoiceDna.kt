package com.example.nextface_android.data

import com.google.gson.annotations.SerializedName

data class VoiceLoginBody (
    @SerializedName("username")
    var userName: String,
    @SerializedName("password")
    var password: String
)

data class VoiceEnrollBody (
    @SerializedName("audios")
    var audios: Array<String>,
    @SerializedName("code")
    var code: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceEnrollBody

        if (!audios.contentEquals(other.audios)) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = audios.contentHashCode()
        result = 31 * result + code.hashCode()
        return result
    }
}

data class VoiceVerifyBody (
    @SerializedName("audio")
    var audio: String,
    @SerializedName("transcript")
    var transcript: Boolean
)

data class VoiceResponse (
    @SerializedName("message")
    var message: String,
)