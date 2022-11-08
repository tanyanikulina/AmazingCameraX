package com.client.alko.common.mlvision.common

import android.graphics.Bitmap
import android.graphics.Canvas
import com.client.alko.common.mlvision.common.GraphicOverlay.Graphic

/** Draw camera image to background.  */
class CameraImageGraphic(overlay: GraphicOverlay, private val bitmap: Bitmap) : Graphic(overlay) {
    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, getTransformationMatrix(), null)
    }
}