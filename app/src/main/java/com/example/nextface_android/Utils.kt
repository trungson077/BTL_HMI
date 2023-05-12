package com.example.nextface_android

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

class Utils {
    fun showDialog(context: Context, content: String){
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                content,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun bitmap2base64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun rotateBitmap(image: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(image, 0,0, image.width, image.height, matrix, true)
    }

    fun generateOTP(): String? {
        return DecimalFormat("000000")
            .format(Random.nextInt(999999))
    }

    fun getDateBefore(timeline: Date, dayOffset: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = timeline
        calendar.add(Calendar.DATE, dayOffset * (-1))
        return calendar.time
    }
}