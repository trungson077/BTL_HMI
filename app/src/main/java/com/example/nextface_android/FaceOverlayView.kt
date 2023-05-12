package com.example.nextface_android

import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import kotlin.math.abs

class FaceOverlayView(
    overlay: GraphicOverlay,
    private val faces: MutableList<Face>,
    private val mapTrackRegconized: MutableMap<Int, String>
) : GraphicOverlay.Graphic(overlay) {

    private val paintLineGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 0, 255, 0)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintLineRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 255, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(50, 0, 255, 0)
        style = Paint.Style.FILL
    }

    private val paintFillUnkown = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(50, 255, 0, 0)
        style = Paint.Style.FILL
    }

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textSize = 40f
    }
    private val paintTextUnkown = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textSize = 40f
    }

    override fun draw(canvas: Canvas) {
        faces.forEach {
            drawFace(canvas, it)
        }
    }

    private fun drawPlotBox(canvas: Canvas,
                            left: Float,
                            top: Float,
                            right: Float,
                            bottom: Float,
                            paint: Paint){
        val width = right - left
        val height = bottom - top

        val lenW = (0.1f * width).toInt()
        val lenH = (0.1f * height).toInt()

        canvas.drawLine(left, top, left+lenW, top, paint)
        canvas.drawLine(left, top, left, top+lenH, paint)

        canvas.drawLine(right, bottom, right-lenW, bottom, paint)
        canvas.drawLine(right, bottom, right, bottom-lenH, paint)

        canvas.drawLine(left, bottom, left+lenW, bottom, paint)
        canvas.drawLine(left, bottom, left, bottom-lenH, paint)

        canvas.drawLine(right, top, right-lenW, top, paint)
        canvas.drawLine(right, top, right, top+lenH, paint)

    }

    private fun drawFace(
        canvas: Canvas,
        face: Face
    ) {
        val x = translateX(face.boundingBox.centerX().toFloat())
        val y = translateY(face.boundingBox.centerY().toFloat())

        // Draws a bounding box around the face.
        val xOffset = (face.boundingBox.width() / 2.0f)
        val yOffset = (face.boundingBox.height() / 2.0f)
        val left: Float = x - xOffset
        val top: Float = y - yOffset
        val right: Float = x + xOffset
        val bottom: Float = y + yOffset
        val centerX: Float = (left + right)/2

        // If face tracking was enabled:
        var isReg: Boolean = false
        if (face.trackingId != -1) {
            val id = face.trackingId
            var label = id.toString()
            var painT = paintTextUnkown

            if(mapTrackRegconized[id].isNullOrEmpty())
                label += "_Unknown"
            else{
                label += "_" + mapTrackRegconized[id]
                painT = paintText
                isReg = true
            }

            canvas.drawText(label, centerX, top, painT)
        }

        if(isReg){
            canvas.drawRect(left, top, right, bottom, paintFill)
            drawPlotBox(canvas, left, top, right, bottom, paintLineGreen)
        }
        else{
            canvas.drawRect(left, top, right, bottom, paintFillUnkown)
            drawPlotBox(canvas, left, top, right, bottom, paintLineRed)
        }
    }
}