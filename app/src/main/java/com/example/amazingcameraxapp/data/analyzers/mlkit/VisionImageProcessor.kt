package com.client.alko.common.mlvision.common

import android.graphics.Bitmap
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.MlKitException

interface VisionImageProcessor {

    /** Processes a bitmap image.  */
    fun processBitmap(
        bitmap: Bitmap?,
        graphicOverlay: GraphicOverlay
//        ,onLargeFaceDetected: (face: Face) -> Unit,
//        onBarcodeDetected: (barcodes: Barcode) -> Unit
    )

    /** Processes ImageProxy image data, e.g. used for CameraX live preview case.  */
    @RequiresApi(VERSION_CODES.KITKAT)
    @Throws(MlKitException::class)
    fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay)

    /** Stops the underlying machine learning model and release resources.  */
    fun stop()
}