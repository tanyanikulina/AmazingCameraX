package com.example.amazingcameraxapp.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.extensions.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.client.alko.common.mlvision.common.GraphicOverlay
import com.client.alko.common.mlvision.common.VisionImageProcessor
import com.example.amazingcameraxapp.data.analyzers.LuminosityAnalyzer
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.demo.kotlin.facedetector.FaceDetectorProcessor
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@SuppressLint("RestrictedApi")
fun createPreviewUseCase(
    aspectRatio: Int,
    previewView: PreviewView,
    selector: CameraSelector,
    needExtension: Boolean = false
): Preview {

    val builder = Preview.Builder()
        .setCameraSelector(selector)
        .setTargetAspectRatio(aspectRatio)

    selector.lensFacing?.let {
        if (needExtension && isExtensionAvailable(it)) {
            val list = getAvailableExtensions(it)
            // just get first available extension for this example
            when (list[0]) {
                ExtensionsManager.EffectMode.NORMAL -> {
                    // do nothing
                }
                ExtensionsManager.EffectMode.BOKEH -> {
                    BokehPreviewExtender.create(builder).enableExtension(selector)
                }
                ExtensionsManager.EffectMode.HDR -> {
                    HdrPreviewExtender.create(builder).enableExtension(selector)
                }
                ExtensionsManager.EffectMode.NIGHT -> {
                    NightPreviewExtender.create(builder).enableExtension(selector)
                }
                ExtensionsManager.EffectMode.BEAUTY -> {
                    BeautyPreviewExtender.create(builder).enableExtension(selector)
                }
                ExtensionsManager.EffectMode.AUTO -> {
                    AutoPreviewExtender.create(builder).enableExtension(selector)
                }
            }
        }
    }

    return builder
        .build()
        .also {
            it.setSurfaceProvider(previewView.surfaceProvider)
            it.imageFormat
        }
}

@SuppressLint("RestrictedApi")
fun createImageCaptureUseCase(
    aspectRatio: Int,
    selector: CameraSelector,
    flashMode: Int,
    needExtension: Boolean = false
): ImageCapture {

    val builder = ImageCapture.Builder()
        .setTargetAspectRatio(aspectRatio)
        .setCameraSelector(selector)
        .setFlashMode(flashMode)

    selector.lensFacing?.let {
        if (needExtension && isExtensionAvailable(it)) {
            val list = getAvailableExtensions(it)
            // just get first available extension for this example
            when (list[0]) {
                ExtensionsManager.EffectMode.NORMAL -> {
                    // do nothing
                }
                ExtensionsManager.EffectMode.BOKEH -> {
                    BokehImageCaptureExtender.create(builder).enableExtension(selector)
                }
                ExtensionsManager.EffectMode.HDR -> {
                    HdrImageCaptureExtender.create(builder).enableExtension(selector)
                }
                ExtensionsManager.EffectMode.NIGHT -> {
                    NightImageCaptureExtender.create(builder).enableExtension(selector)
                }
                ExtensionsManager.EffectMode.BEAUTY -> {
                    BeautyImageCaptureExtender.create(builder).enableExtension(selector)
                }
                ExtensionsManager.EffectMode.AUTO -> {
                    AutoImageCaptureExtender.create(builder).enableExtension(selector)
                }
            }
        }
    }

    return builder.build()
}

@SuppressLint("RestrictedApi")
fun createVideoCaptureUseCase(aspectRatio: Int, selector: CameraSelector): VideoCapture {
    return VideoCapture.Builder().apply {
        setCameraSelector(selector)
        // The target aspect ratio is used as a hint when determining the resulting output aspect
        // ratio which may differ from the request, possibly due to device constraints.
        // See the documentation
        setTargetAspectRatio(aspectRatio)
    }
        .build()
}

fun createLumaAnalysisUseCase(
    context: Context,
    aspectRatio: Int,
    onResultAvailable: (Double) -> Unit
): ImageAnalysis {
    val analysisUseCase = ImageAnalysis.Builder()
        .setTargetAspectRatio(aspectRatio)
        .build()
    analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context), LuminosityAnalyzer {
        onResultAvailable(it)
    })
    return analysisUseCase
}

fun createFaceDetectorProcessor(
    context: Context,
    onResultAvailable: (faces: List<Face>) -> Unit
): VisionImageProcessor {
    val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setMinFaceSize(0.6f)
        .enableTracking()
        .build()

    val faceDetectorProcessor: VisionImageProcessor =
        FaceDetectorProcessor(context, faceDetectorOptions) {
            onResultAvailable(it)
        }

    return faceDetectorProcessor
}

fun createFaceDetectorUseCase(
    context: Context,
    aspectRatio: Int,
    selector: CameraSelector,
    visionImageProcessor: VisionImageProcessor,
    graphicOverlay: GraphicOverlay
): ImageAnalysis {

    val analysisUseCase = ImageAnalysis.Builder()
        .setTargetAspectRatio(aspectRatio)
        .build()

    analysisUseCase.setAnalyzer(
        ContextCompat.getMainExecutor(context),
        ImageAnalysis.Analyzer { imageProxy ->
            val isImageFlipped = selector == CameraSelector.DEFAULT_FRONT_CAMERA
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees == 0 || rotationDegrees == 180) {
                graphicOverlay.setImageSourceInfo(
                    imageProxy.width,
                    imageProxy.height,
                    isImageFlipped
                )
            } else {
                graphicOverlay.setImageSourceInfo(
                    imageProxy.height,
                    imageProxy.width,
                    isImageFlipped
                )
            }

            try {
                visionImageProcessor.processImageProxy(imageProxy, graphicOverlay)
            } catch (e: MlKitException) {
                Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }

        })

    return analysisUseCase
}

//fun createAnalysisUseCase(
//    aspectRatio: Int,
//    context: Context,
//    graphicOverlay: GraphicOverlay,
//    imageProcessor: VisionImageProcessor,
//    isCameraFront: Boolean,
//    updateGraphicOverlayImageSourceInfo: Boolean
//): ImageAnalysis {
//
//    val analysisUseCase = ImageAnalysis.Builder()
//        .setTargetAspectRatio(aspectRatio)
//        .build()
//
//    analysisUseCase.setAnalyzer(
//        // imageProcessor.processImageProxy will use another thread to run the detection underneath,
//        // thus we can just runs the analyzer itself on main thread.
//        ContextCompat.getMainExecutor(context),
//        ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
//            if (updateGraphicOverlayImageSourceInfo) {
//                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
//                if (rotationDegrees == 0 || rotationDegrees == 180) {
//                    graphicOverlay.setImageSourceInfo(
//                        imageProxy.width, imageProxy.height, isCameraFront
//                    )
//                } else {
//                    graphicOverlay.setImageSourceInfo(
//                        imageProxy.height, imageProxy.width, isCameraFront
//                    )
//                }
//            }
//            try {
//                imageProcessor.processImageProxy(imageProxy, graphicOverlay)
//            } catch (e: MlKitException) {
//                Timber.e("Failed to process image. Error: " + e.localizedMessage)
//                Toast.makeText(graphicOverlay.context, e.localizedMessage, Toast.LENGTH_SHORT).show()
//            }
//        }
//    )
//    return analysisUseCase
//}

// Get screen metrics used to setup camera for full screen resolution
fun createScreenAspectRatio(previewView: PreviewView): Int {
    val RATIO_4_3_VALUE = 4.0 / 3.0
    val RATIO_16_9_VALUE = 16.0 / 9.0

    val metrics = DisplayMetrics().also {
        previewView.display.getRealMetrics(it)
    }
    val previewRatio = max(metrics.widthPixels, metrics.heightPixels).toDouble() / min(
        metrics.widthPixels,
        metrics.heightPixels
    )
    if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE))
        return AspectRatio.RATIO_4_3
    return AspectRatio.RATIO_16_9
}

fun getAvailableExtensions(lensFacing: Int): List<ExtensionsManager.EffectMode> {
    val list = ArrayList<ExtensionsManager.EffectMode>()
    ExtensionsManager.EffectMode.values().forEach {
        if (it != ExtensionsManager.EffectMode.NORMAL) {
            val hasExt = ExtensionsManager.isExtensionAvailable(it, lensFacing)
            if (hasExt) list.add(it)
        }
    }
    return list.sortedBy { it.name }
}

@SuppressLint("RestrictedApi")
fun isExtensionAvailable(lensFacing: Int): Boolean {
//    val extensionsManager: Extensions = ExtensionsManager.getExtensions(context)
    return getAvailableExtensions(lensFacing).isNotEmpty()
}