package com.example.amazingcameraxapp.data.analyzers

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.amazingcameraxapp.common.toByteArray
import java.util.*
import kotlin.collections.ArrayList


typealias LumaListener = (luma: Double) -> Unit

internal class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {

    private val frameRateWindow = 8
    private val frameTimestamps = ArrayDeque<Long>(5)
    private var lastAnalyzedTimestamp = 0L
    private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }

    var framesPerSecond: Double = -1.0
        private set

    fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

    override fun analyze(image: ImageProxy) {

        if (listeners.isEmpty()) {
            image.close()
            return
        }

        // Keep track of frames analyzed
        val currentTime = System.currentTimeMillis()
        frameTimestamps.push(currentTime)

        // Compute the FPS using a moving average
        while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
        val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
        val timestampLast = frameTimestamps.peekLast() ?: currentTime
        framesPerSecond =
            1.0 / ((timestampFirst - timestampLast) / frameTimestamps.size.coerceAtLeast(1)
                .toDouble()) * 1000.0

        // Analysis could take an arbitrarily long amount of time
        // Since we are running in a different thread, it won't stall other use cases

        lastAnalyzedTimestamp = frameTimestamps.first

        // Compute average luminance for the image
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()

        listeners.forEach { it(luma) }

        image.close()
    }
}