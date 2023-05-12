package com.example.nextface_android

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.nextface_android.api.APIClient
import com.example.nextface_android.api.NFCallback
import com.example.nextface_android.data.FaceSearchRequest
import com.example.nextface_android.data.LoginRequest
import com.example.nextface_android.data.LoginResponse
import com.example.nextface_android.model.StaffInfo
import com.google.gson.Gson
import com.google.mlkit.vision.face.Face
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.abs
import kotlin.math.log

class FaceSearch(
    private val context: Context,
    private val apiClient: APIClient,
    private val logService: LogService,
    private var mapTrackRecognized: MutableMap<Int, String>,
) {
    private var httpClient = OkHttpClient()
    private var authToken : String = ""
    private var trackRecognizing: MutableList<Int> = mutableListOf()
    var codeList : MutableList<String> = mutableListOf()

    private val util = Utils()
    private val offset = 30

    fun getAuthorToken() : String{
        return authToken
    }

    fun loginApiFaceSearch(){
        logService.appendLog("call login api face search")
        apiClient.getApiService(Constants.BASE_FS_URL).login(
            LoginRequest(user = Constants.API_FS_USER,
            password = Constants.API_FS_PASSWORD,
            serialNumber = Constants.serialNumber)
        )
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    val loginResponse = response.body()
                    if(loginResponse?.authToken.isNullOrEmpty()){
                        logService.appendLog("Code:${response.code()} login api face search get authToken: " + loginResponse?.authToken)
                        util.showDialog(context,"Login API Face search failure")
                    }else{
                        logService.appendLog("Code:${response.code()} login api face search success, authToken = " + loginResponse?.authToken)
                        authToken = loginResponse?.authToken.toString()
                        util.showDialog(context, "Login API Face search Success")
                    }

                    Log.d("verifyWorkingSession", "============login: authToken = ${authToken}")
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    logService.appendLog("login api face search failure")
                    util.showDialog(context,"Login API Face search failure")
                }
            })
    }

    fun doFaceSearchRequest(faceList: MutableList<Face>,
                            image: Bitmap,
                            callback: (staff: StaffInfo) -> Unit
    ) {
        val rotateBm = util.rotateBitmap(image, -90f)
        for(face in faceList){
            val trackId = face.trackingId ?: continue

            // check is has recognized
            if(!checkIsNotRecognized(trackId))
                continue

            // check is recognizing
            if(trackRecognizing.contains(trackId)) continue

            if(mapTrackRecognized.size > 10)
                mapTrackRecognized.clear()

            val left = maxOf(0, face.boundingBox.left)
            val top = maxOf(0, face.boundingBox.top)
            val right = minOf(face.boundingBox.right, rotateBm.width)
            val bottom = minOf(face.boundingBox.bottom, rotateBm.height)
            val width = maxOf(right - left, 0)
            val height = maxOf(bottom - top, 0)

            // check min face size
            if (width <= Constants.MIN_FACE || height <= Constants.MIN_FACE) continue

            if(!checkFrontFace(face)) continue

            // crop face
            val croppedBitmap = Bitmap.createBitmap(rotateBm, left, top, width, height)
            val base64Encoded = util.bitmap2base64(croppedBitmap)

            // request
            val jsonData = Gson().toJson(
                FaceSearchRequest(limit=1, imageBase64 = base64Encoded,
                serialNumber = Constants.serialNumber)
            )
            val body = jsonData.toString().toRequestBody()

            val request = Request.Builder()
                .method("POST",body)
                .url(Constants.BASE_FS_URL + Constants.API_FS_SEARCH_BASE64)
                .header("Content-Type","application/json")
                .header("Authorization","Bearer $authToken")
                .build()

            // add to recognizing list
            trackRecognizing.add(trackId)

            // call API
            logService.appendLog("call face search request")
            httpClient.newCall(request).enqueue(object : okhttp3.Callback{
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    logService.appendLog("Request face search error ${e.message.toString()}")
                    trackRecognizing.remove(trackId)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    trackRecognizing.remove(trackId)
                    if(response.code == 401)
                    {
                        loginApiFaceSearch()
                        throw IOException("Unexpected code $response")
                    }
                    try{
                        val responseData = response.body?.string()
                        val data = JSONArray(responseData)
                        logService.appendLog("FS resp $data")
                        val jsonObj = data.getJSONObject(0)
                        val name = jsonObj.optString("name")
                        val similarity = jsonObj.optString("similarity").toFloat()
                        val code = jsonObj.optString("code")

                        if(!codeList.contains(code)) {
                            codeList.add(code)
                        }

                        if (similarity >= Constants.REG_THRESH){
                            mapTrackRecognized[trackId] = name

                            val imgLeft = maxOf(0, face.boundingBox.left - offset)
                            val imgTop = maxOf(0, face.boundingBox.top - offset)
                            val imgRight = minOf(face.boundingBox.right + offset, rotateBm.width)
                            val imgBottom = minOf(face.boundingBox.bottom + offset, rotateBm.height)
                            val imgW = maxOf(imgRight - imgLeft, 0)
                            val imgH = maxOf(imgBottom - imgTop, 0)
                            val imgBitmap = Bitmap.createBitmap(rotateBm, imgLeft, imgTop, imgW, imgH)

                            val staff = StaffInfo(name, imgBitmap, code)
                            callback(staff)
                        }

                    }catch (e: JSONException){
                        logService.appendLog("JSONException: ${e.message.toString()}")
                    }catch (e: Exception){
                        logService.appendLog("Other exception: ${e.message.toString()}")
                    }
                }
            })
        }

        return
    }

    private fun checkFrontFace(face: Face): Boolean{
        val isFrontal = true
        if(abs(face.headEulerAngleX) > Constants.max_head_x ||
            abs(face.headEulerAngleY) > Constants.max_head_y ||
            abs(face.headEulerAngleZ) > Constants.max_head_z)
            return false
        return isFrontal
    }

    private fun checkIsNotRecognized(trackId: Int): Boolean{
        return mapTrackRecognized[trackId].isNullOrEmpty()
    }

    fun verifyUser(code: String, callback: NFCallback<String>) {
        val url = (Constants.BASE_FS_URL + Constants.API_FS_SELECT).toHttpUrlOrNull()
            ?.newBuilder()?.apply { addQueryParameter("code", code) }
            ?.build()
        val req = Request.Builder()
            .method("GET", null)
            .url(url!!)
            .header("Content-Type","application/json")
            .header("Authorization","Bearer $authToken")
            .build()
        try {
            httpClient.newCall(req).enqueue(object: okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: okio.IOException) {
                    callback.onFailure(e.message.toString())
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        if (response.code == 200) {
                            val jsonObject = JSONObject(response.body?.string() ?: "")
                            val isKey = jsonObject.getBoolean("is_key")
                            if(isKey)
                                callback.onSuccess("User is authorized")
                            else
                                callback.onFailure("User is not authorized")
                        } else {
                            callback.onFailure("User is not authorized")
                        }
                    } catch (e: JSONException) {
                        logService.appendLog("FACE SEARCH ${e.message.toString()}")
                        callback.onFailure(e.message.toString())
                    } catch (e: IOException) {
                        logService.appendLog("FACE SEARCH ${e.message.toString()}")
                        callback.onFailure(e.message.toString())
                    }
                }
            })
        }
        catch (e: Exception){
            println(e)
        }

    }

    fun verifyWorkingSession(callback: NFCallback<String>){
        var verify_result = true
        val url = (Constants.BASE_FS_URL + Constants.API_IS_SESSION_TIME).toHttpUrlOrNull()
        val req = Request.Builder()
            .method("GET", null)
            .url(url!!)
            .header("Content-Type","application/json")
            .header("Authorization","Bearer $authToken")
            .build()
        Log.d("verifyWorkingSession", "============authToken = ${authToken}")

        try {
            httpClient.newCall(req).enqueue(object: okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: okio.IOException) {
                    callback.onFailure(e.message.toString())
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        Log.d("verifyWorkingSession", "============res code = ${response.code}")
                        if (response.code == 200) {
                            verify_result = response.body?.string().toBoolean()
                            Log.d("verifyWorkingSession", "============res= $verify_result")

                            if (!verify_result){
                                callback.onSuccess(verify_result.toString())
                            }else{
                                callback.onSuccess(verify_result.toString())
                            }
                        }
                    } catch (e: JSONException) {
                        Log.d("verifyWorkingSession", "============err: $e")
                        callback.onFailure(e.message.toString())
                    } catch (e: IOException) {
                        Log.d("verifyWorkingSession", "============err: $e")
                        callback.onFailure(e.message.toString())
                    }
                }
            })
        }catch (e: Exception){
            println(e)
        }
    }
}