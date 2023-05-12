package com.example.nextface_android.model

import android.graphics.Bitmap

class StaffInfo(name: String, avatar: Bitmap?, code: String) {
    var name: String? = name
    var avatar: Bitmap? = avatar
    var code: String? = code

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StaffInfo

        if (name != other.name) return false
        if (avatar != other.avatar) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (avatar?.hashCode() ?: 0)
        result = 31 * result + (code?.hashCode() ?: 0)
        return result
    }
}