package com.example.nextface_android

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build

object Constants {
    // Face search API
    const val BASE_FS_URL = "http://103.160.75.240:30093/api/v1/"
    const val API_FS_USER = "bimbip"
    const val API_FS_PASSWORD = "123456aA@"
    const val API_FS_LOGIN_URL = "auth/login"
    const val API_FS_SEARCH_BASE64 = "faces/search"
    const val API_FS_SELECT = "faces/select"
    const val API_IS_SESSION_TIME = "users/is_session_time"

    /*
    *  Voice API
    */
    const val BASE_VD_URL = "https://voicedna-api.namisense.ai"
    const val API_VD_USER = "nextpoc"
    const val API_VD_PASS = "qsydLVQM49gj"
    const val API_VD_LOGIN = "/login"
    const val API_VD_ENROLL = "/voice/enroll"
    const val API_VD_VOICES = "/voices"
    const val API_VD_VERIFY = "/voice/verify"

    const val ENABLE_SAVE_LOG = false
    const val API_SAVE_LOG_FILE = "file/log-hoya?serial="

    const val MIN_FACE = 100
    const val DETECT_INTERVAL = 10
    const val REG_THRESH = 0.75f

    const val KEY_FACE_NUMB = 1

    // Frontal face
    // Max value of abs(face.headEulerAngleX)
    const val max_head_x = 25
    // Max value of abs(face.headEulerAngleY)
    const val max_head_y = 30
    // Max value of abs(face.headEulerAngleZ)
    const val max_head_z = 30


    /*
    *  GATE
    */
    const val REQUEST_GATE_CLOSE = "1"
    const val REQUEST_GATE_OPEN = "2"
    const val REQUEST_GATE_STATUS = "3"
    const val WRITE_WAIT_MILLIS = 500
    const val READ_WAIT_MILLIS = 500
    const val DISTANCE_TO_CLOSE_GATE = 200


    @SuppressLint("HardwareIds")
    var serialNumber: String = Build.SERIAL.toString()

    const val REQUEST_CODE_PERMISSIONS = 10
    val CAMERA_REQUIRED_PERMISSIONS =
        mutableListOf (
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

    val RECORD_AUDIO_REQUIRED_PERMISSIONS =
        mutableListOf (
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
}