package com.example.nextface_android.model

import android.content.Context
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.example.nextface_android.Constants
import com.example.nextface_android.LogService
import com.example.nextface_android.R
import com.example.nextface_android.Utils
import com.example.nextface_android.api.NFCallback
import com.example.nextface_android.data.VoiceEnrollBody
import com.example.nextface_android.data.VoiceLoginBody
import com.example.nextface_android.data.VoiceVerifyBody
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*


class VoiceModel(
    private val context: Context,
    private val logService: LogService,
    private val httpClient: OkHttpClient
) {
    private var mediaRecorder: MediaRecorder? = null

    private var authToken: String = ""
    private val util = Utils()

    init {
        createMediaRecorder()
    }

    private val recordingFilePath: String
        get() = "${context.externalCacheDir?.absolutePath}/recording_temp.3gpp"

    fun loginVoiceService() {
        logService.appendLog("call login api for voice service")
        val body = Gson().toJson(VoiceLoginBody(Constants.API_VD_USER, Constants.API_VD_PASS))
        val request = Request.Builder()
            .method("POST", body.toRequestBody())
            .url(Constants.BASE_VD_URL + Constants.API_VD_LOGIN)
            .header("Accept","application/json")
            .header("Content-Type","application/json")
            .build()
        httpClient.newCall(request).enqueue(object: okhttp3.Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val jsonBody = JSONObject(response.body.string())
                        val token = jsonBody.getString("token")
                        if(token.isNullOrEmpty()) {
                            logService.appendLog("Login VoiceDNA failure with code ${response.code}")
                            util.showDialog(context, "Login VoiceDNA failure")
                        } else {
                            logService.appendLog("Login VoiceDNA Success with code: ${response.code}")
                            authToken = token
                            util.showDialog(context, "Login VoiceDNA Success")
                        }
                    } catch (e: JSONException) {
                        logService.appendLog("VOICE DNA Login ${e.message.toString()}")
                    } catch (e: Exception) {
                        logService.appendLog("VOICE DNA Login ${e.message.toString()}")
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    logService.appendLog("Login VoiceDNA failure ${e.message.toString()}")
                    util.showDialog(context, "Login VoiceDNA failure")
                }
            }
        )
    }

    fun enrollVoice(audios: Array<String>, code: String, callback: NFCallback<String>) {
        val body = Gson().toJson(VoiceEnrollBody(audios, code))
        val request = Request.Builder()
            .method("POST", body.toRequestBody())
            .url(Constants.BASE_VD_URL + Constants.API_VD_ENROLL)
            .header("Accept","application/json")
            .header("Content-Type","application/json")
            .header("Authorization","Bearer $authToken")
            .build()
        httpClient.newCall(request).enqueue(object: okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                logService.appendLog("VOICE DNA Enroll ${e.message.toString()}")
                callback.onFailure(e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    when (response.code) {
                        200 -> {
                            val jsonBody = JSONObject(response.body.string())
                            val status = jsonBody.optInt("status")
                            Log.d("enrollVoice","======= ${jsonBody}")

                            var msg = context.getString(R.string.registerFailed)
                            if(status == 0)
                                msg = context.getString(R.string.registerSuccessfully)
                            if(status == 1)
                                msg = context.getString(R.string.invalidAudio) // "Invalid audio"
                            if(status == 2)
                                msg = context.getString(R.string.shortSpeech)//"Short speech"
                            if(status == 5)
                                msg = context.getString(R.string.voiceExist) //"Voice exist"
                            callback.onSuccess(msg)
                        }
                        422 -> {
                            val jsonBody = JSONObject(response.body.string())
                            val detail = jsonBody.getJSONArray("detail").get(0)
                            val msg = JSONObject(detail.toString()).getString("msg")
                            callback.onFailure(msg)
                        }
                        else -> {
                            val jsonBody = JSONObject(response.body.string())
                            val msg = jsonBody.getString("detail")
                            callback.onFailure(msg)
                        }
                    }
                } catch (e: JSONException) {
                    logService.appendLog("VOICE DNA Enroll ${e.message.toString()}")
                } catch (e: Exception) {
                    logService.appendLog("VOICE DNA Enroll ${e.message.toString()}")
                }
            }

        })
    }

    fun verifyVoice(code: String, audio: String,
                    description: String = "String", callback: NFCallback<String>
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault(Locale.Category.FORMAT))
        val now = Date()
        val from = sdf.format(util.getDateBefore(now, 30))
        val to = sdf.format(now)
        var voiceID: String? = null
        getAllVoices(0,10, code, from, to, object: NFCallback<String> {
            override fun onSuccess(response: String) {
                voiceID = response

                val url = Constants.BASE_VD_URL + Constants.API_VD_VERIFY + "/" + voiceID
                val body = Gson().toJson(VoiceVerifyBody(audio, false))
                val request = Request.Builder()
                    .method("POST", body.toRequestBody())
                    .url(url)
                    .header("Accept","application/json")
                    .header("Content-Type","application/json")
                    .header("Authorization","Bearer $authToken")
                    .build()
                httpClient.newCall(request).enqueue(object: okhttp3.Callback {
                    override fun onFailure(call: Call, e: IOException) {
                    logService.appendLog("VOICE DNA VerifyVoice ${e.message.toString()}")
                        callback.onFailure(e.message.toString())
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            when (response.code) {
                                200 -> {
                                    val jsonBody = JSONObject(response.body.string())
                                    val status = jsonBody.optInt("status")
                                    Log.d("verifyVoice","======= ${jsonBody}")
//                                    callback.onSuccess(context.getString(R.string.identificationSuccessfully))
                                    callback.onSuccess(status.toString())
                                }
                                422 -> {
                                    val jsonBody = JSONObject(response.body.string())
                                    val detail = jsonBody.getJSONArray("detail").get(0)
                                    val msg = JSONObject(detail.toString()).getString("msg")
                                    callback.onFailure(msg)
                                }
                                else -> {
                                    val jsonBody = JSONObject(response.body.string())
                                    val msg = jsonBody.getString("detail")
                                    callback.onFailure(msg)
                                }
                            }
                        } catch (e: JSONException) {
                            logService.appendLog("VOICE DNA VerifyVoice ${e.message.toString()}")
                        } catch (e: Exception) {
                            logService.appendLog("VOICE DNA VerifyVoice ${e.message.toString()}")
                        }
                    }
                })
            }
            override fun onFailure(message: String) {
                logService.appendLog("VOICE DNA VerifyVoice $message")
                callback.onFailure(message)
            }
        })
    }

    private fun getAllVoices(
        offset: Int = 0,
        limit: Int = 10,
        code: String,
        from: String,
        to: String,
        callback: NFCallback<String>
    ) {
        val url = (Constants.BASE_VD_URL + Constants.API_VD_VOICES).toHttpUrlOrNull()
            ?.newBuilder()?.apply {
                addQueryParameter("offset", "$offset")
                addQueryParameter("limit", "$limit")
                addQueryParameter("code", code)
                addQueryParameter("from", from)
                addQueryParameter("to", to)
            }?.toString()
        val req = Request.Builder()
            .method("GET", null)
            .url(url!!)
            .header("Accept","application/json")
            .header("Authorization","Bearer $authToken")
            .build()

        httpClient.newCall(req).enqueue(object: okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    when (response.code) {
                        200 -> {
                            val jsonBody = JSONArray(response.body.string())
                            if(jsonBody.length() > 0) {
                                val firstElement = JSONObject(jsonBody.get(0).toString())
                                val voiceId = firstElement.getString("id")
                                callback.onSuccess(voiceId)
                            } else {
                                callback.onFailure(context.getString(R.string.userNotRegisteredVoice))
                            }
                        }
                        422 -> {
                            val jsonBody = JSONObject(response.body.string())
                            val detail = jsonBody.getJSONArray("detail")
                            val msg = detail.toString()
                            callback.onFailure(msg)
                        }
                        else -> {
                            val jsonBody = JSONObject(response.body.string())
                            val msg = jsonBody.getString("detail")
                            callback.onFailure(msg)
                        }
                    }
                } catch (e: JSONException) {
                    logService.appendLog("VOICE DNA GetAllVoices ${e.message.toString()}")
                } catch (e: IOException) {
                    logService.appendLog("VOICE DNA GetAllVoices ${e.message.toString()}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                logService.appendLog("VOICE DNA GetAllVoices ${e.message.toString()}")
                callback.onFailure(e.message.toString())
            }
        })
    }

    private fun createMediaRecorder() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(recordingFilePath)
        }
    }

    fun startRecord() {
        mediaRecorder?.apply {
            try {
                prepare()
                start()
            } catch (e: IOException) {
                logService.appendLog("MediaRecorder.start() failed")
                logService.appendLog(e.toString())
            }
        }
    }

    fun stopRecord() {
        mediaRecorder?.apply {
            try {
                stop()
                reset()
                release()
            } catch (e: IOException) {
                logService.appendLog("MediaRecorder.stop() failed")
                logService.appendLog(e.toString())
            }
        }
    }

    fun queryRecordedData(): String? {
        val file = File(recordingFilePath)
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            logService.appendLog(" Voice DNA QueryRecorder ${e.message.toString()}")
        } catch (e: IOException) {
            logService.appendLog(" Voice DNA QueryRecorder ${e.message.toString()}")
        }

        return Base64.encodeToString(
            bytes,
            Base64.DEFAULT
        )
    }
}