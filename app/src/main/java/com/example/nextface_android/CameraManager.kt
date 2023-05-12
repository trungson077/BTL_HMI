package com.example.nextface_android

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.nextface_android.api.APIClient
import com.example.nextface_android.model.StaffInfo
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class CameraManager(
    private val context: Context,
    private val finderView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private val graphicOverlay: GraphicOverlay,
    private val pJob: Job, override val coroutineContext: CoroutineContext,
) : CoroutineScope {
    private lateinit var mJob: Job

    private var preview: Preview? = null

    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraSelectorOption = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null

    private var imageAnalyzer: ImageAnalysis? = null

    private var faceAnalyserMode = FaceAnalyser.REAL_TIME

    private var logService = LogService
    private var mapTrackRecognized: MutableMap<Int, String> = mutableMapOf()

    private var apiClient: APIClient
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    var faceSearch: FaceSearch

    init {
        createNewExecutor()
        apiClient = APIClient()

        cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        faceSearch = FaceSearch(context, apiClient, logService, mapTrackRecognized)
        faceSearch.loginApiFaceSearch()
    }

    private fun createNewExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun startCamera() {
        startCamera { logService.appendLog(it.name.toString()) }
    }

    fun startCamera(callback: ((staff: StaffInfo) -> Unit)) {
        cameraProviderFuture.addListener({ cameraHandler(cameraProviderFuture, callback) },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun stopCamera() {
        cameraProviderFuture.cancel(true)
        mapTrackRecognized.clear()
    }

    private fun cameraHandler(cameraProviderFeature: ListenableFuture<ProcessCameraProvider>,
                              callback: ((staff: StaffInfo) -> Unit)) {
        val screenW = 480
        val screenH = 640
        val rotation = Surface.ROTATION_90
        var frameCount = 0

        cameraProvider = cameraProviderFeature.get()
        preview = Preview.Builder()
            .setTargetResolution(Size(screenW, screenH))
            .setTargetRotation(rotation)
            .build().also {
                it.setSurfaceProvider(finderView.surfaceProvider)
                finderView.scaleX = -1f
            }
        graphicOverlay.setPreviewSize(screenW, screenH)

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(screenW, screenH))
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    FaceAnalyser(faceAnalyserMode) { faceList, image ->
                        createOverlays(faceList)
                        frameCount++
                        if(frameCount % Constants.DETECT_INTERVAL == 0) {
                            requestFaceReg(faceList, image, callback)
                        }
                        if(frameCount % 1000 == 0)
                            logService.appendLog("Process is running")
                        if(frameCount >= 10000)
                            frameCount = 1
                        uploadFileLog()

                        val codeIterator = faceSearch.codeList.iterator()
                        while (codeIterator.hasNext()){
                            val code = codeIterator.next()
//                                    requestOpenDoor(code)
                            codeIterator.remove()
                        }
                    })
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(cameraSelectorOption)
            .build()

        setCameraConfig(cameraProvider, cameraSelector)
    }

    private fun requestFaceReg(faceList: MutableList<Face>, image: Bitmap,
                               callback: ((staff: StaffInfo) -> Unit)
    ){
        mJob = Job(pJob)
        launch(Dispatchers.IO) {
            val one = async { faceSearch.doFaceSearchRequest(faceList, image, callback) }
            one.await()
        }
    }

    private fun uploadFileLog(){
        mJob = Job(pJob)
        val fileLog = logService.getFileLog2Upload()
        // check to upload file log to S3
        if(!fileLog.isNullOrEmpty()){
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, fileLog)
            if(file.exists()){
                launch(Dispatchers.IO) {
                    val one = async {
                        logService.uploadFileLog(file, fileLog, faceSearch.getAuthorToken())
                    }
                    one.await()
                }
            }
        }
    }

    private fun setCameraConfig(
        cameraProvider: ProcessCameraProvider?,
        cameraSelector: CameraSelector
    ) {
        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun createOverlays(faceList: MutableList<Face>){
        graphicOverlay.clear()
        val overlayView = FaceOverlayView(graphicOverlay, faceList, mapTrackRecognized)
        graphicOverlay.add(overlayView)
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}