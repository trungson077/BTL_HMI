package com.example.nextface_android.viewmodel

import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.nextface_android.model.StaffInfo

class NextFaceViewModel: ViewModel() {
    private var staff: StaffInfo? = null
    private var result: Result? = null

    fun setStaff(info: StaffInfo?) {
        staff = info
    }
    fun getStaff(): StaffInfo? {
        return staff
    }

    fun setResult(msg: String?, color: Int = Color.GREEN) {
        if(result == null) {
            result = Result()
        }
        result?.text = msg
        result?.color = color
    }
    fun getResult(): Result? {
        return result
    }

    fun reset() {
        staff = null
        result = null
    }

    class Result {
        var text: String? = null
        var color: Int? = null
    }
}