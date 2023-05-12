package com.example.nextface_android

import android.annotation.SuppressLint
import android.graphics.*
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


class FaceAnalyser(
    mode: Int,
    private val listener: (MutableList<Face>, Bitmap) -> Unit
) : ImageAnalysis.Analyzer {
   private val highAccuracyOpts by lazy {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
    }

    private val realTimeOpts by lazy {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.5f)
            .enableTracking()
            .build()
    }

    private val detector = FaceDetection.getClient(
        when (mode){
            HIGH_ACCURACY ->highAccuracyOpts
            REAL_TIME -> realTimeOpts
            else -> throw Exception("Invalid mode selected")
        }
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: kotlin.run{
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener {
                removeSmallFace(it)
                val bitmapImage = toBitmap(mediaImage)
                if (bitmapImage != null) {
                    listener.invoke(it, bitmapImage)
                }
            }
            .addOnFailureListener { Log.e(TAG, "Error: ${it.message}", it) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun removeSmallFace(faceList : MutableList<Face>){
        val iterator = faceList.iterator()
        while(iterator.hasNext()){
            val face = iterator.next()
            val fWidth = face.boundingBox.width()
            val fHeight = face.boundingBox.height()
            if(fWidth < Constants.MIN_FACE || fHeight < Constants.MIN_FACE)
                iterator.remove()
        }
    }

    private fun toBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val yBuffer: ByteBuffer = planes[0].buffer
        val uBuffer: ByteBuffer = planes[1].buffer
        val vBuffer: ByteBuffer = planes[2].buffer
        val ySize: Int = yBuffer.remaining()
        val uSize: Int = uBuffer.remaining()
        val vSize: Int = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes: ByteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    companion object{
        private const val TAG = "FaceAnalyser"
        const val HIGH_ACCURACY = 0
        const val REAL_TIME = 1
    }
}