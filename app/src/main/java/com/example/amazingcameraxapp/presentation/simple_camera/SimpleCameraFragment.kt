package com.example.amazingcameraxapp.presentation.simple_camera

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.example.amazingcameraxapp.R
import com.example.amazingcameraxapp.common.createImageDateFile
import com.example.amazingcameraxapp.common.createVideoDateFile
import com.example.amazingcameraxapp.common.getOutputImageDirectory
import com.example.amazingcameraxapp.data.*
import kotlinx.android.synthetic.main.simple_camera_fragment.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SimpleCameraFragment : Fragment(R.layout.simple_camera_fragment) {

    private val TAG = "SimpleCameraFragment"
    private lateinit var viewModel: SimpleCameraViewModel

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null

    private var useCaseGroup: UseCaseGroup? = null
    private var isVideoStarted = false

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraSelectorState: CameraSelectorState = CameraSelectorState.BackCameraState
    private var captureState: CaptureState = CaptureState.PhotoState
    private var flashState: FlashState = FlashState.FlashStateOff
    private var extensionState: ExtensionState = ExtensionState.ExtensionStateOff
    private var aspectRatioState: AspectRatioState = AspectRatioState.AspectRatio_16_9
    private var screenAspectRatio: Int = AspectRatio.RATIO_16_9

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var videoExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        videoExecutor = Executors.newSingleThreadExecutor()
    }

    @SuppressLint("RestrictedApi")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider.AndroidViewModelFactory
            .getInstance(requireActivity().application)
            .create(SimpleCameraViewModel::class.java)

        viewModel.cameraProviderLiveData.observe(requireActivity()) {
            cameraProvider = it
            startCamera()
            cvControl.isVisible = true
        }

        outputDirectory = requireContext().getOutputImageDirectory()
        checkExtensions()

        ivSwitchCamera.setOnClickListener {
            if (!isVideoStarted) {
                when (cameraSelectorState) {
                    CameraSelectorState.FrontCameraState -> {
                        cameraSelectorState = CameraSelectorState.BackCameraState
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    CameraSelectorState.BackCameraState -> {
                        cameraSelectorState = CameraSelectorState.FrontCameraState
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                }
                checkExtensions()
                startCamera()
            }
        }
        ivCapture.setOnClickListener {
            when (captureState) {
                CaptureState.VideoState -> {
                    isVideoStarted = !isVideoStarted
                    if (isVideoStarted) {
                        startVideo()
                        Toast.makeText(requireContext(), "Video started", Toast.LENGTH_SHORT).show()
                    } else {
                        stopVideo()
                        Toast.makeText(requireContext(), "Video stopped", Toast.LENGTH_SHORT).show()
                    }
                }
                CaptureState.PhotoState -> {
                    takePhoto()
                }
            }
        }
        tvPhoto.setOnClickListener {
            if (!isVideoStarted) {
                captureState = CaptureState.PhotoState
                cameraProvider.unbind(videoCapture)
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
                tvPhoto.setTextColor(resources.getColor(R.color.white))
                tvVideo.setTextColor(resources.getColor(R.color.gray))
                ivFlash.isVisible = true
                checkExtensions()
            }
        }
        tvVideo.setOnClickListener {
            if (!isVideoStarted) {
                captureState = CaptureState.VideoState
                cameraProvider.unbind(imageCapture)
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)
                tvVideo.setTextColor(resources.getColor(R.color.white))
                tvPhoto.setTextColor(resources.getColor(R.color.gray))
                ivFlash.isVisible = false
                ivExtension.isVisible = false
            }
        }
        ivFlash.setOnClickListener {
            if (captureState != CaptureState.VideoState)
                when (flashState) {
                    FlashState.FlashStateOn -> {
                        flashState = FlashState.FlashStateOff
                        ivFlash.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_flash_off_24))
                        changeImageCaptureUsecase(ImageCapture.FLASH_MODE_OFF)
                    }
                    FlashState.FlashStateOff -> {
                        flashState = FlashState.FlashStateAuto
                        ivFlash.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_flash_auto_24))
                        changeImageCaptureUsecase(ImageCapture.FLASH_MODE_AUTO)
                    }
                    FlashState.FlashStateAuto -> {
                        flashState = FlashState.FlashStateOn
                        ivFlash.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_flash_on_24))
                        changeImageCaptureUsecase(ImageCapture.FLASH_MODE_ON)
                    }
                }
            // how to use flash as a lantern
//            camera.cameraControl.enableTorch(flashState!= FlashState.FlashStateOff)
        }
        ivFormat.setOnClickListener {
            if (!isVideoStarted) {
                when (aspectRatioState) {
                    AspectRatioState.AspectRatio_16_9 -> {
                        aspectRatioState = AspectRatioState.AspectRatio_4_3
                        screenAspectRatio = AspectRatio.RATIO_4_3
                        ivFormat.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_crop_4_3_24))
                        startCamera()
                    }
                    AspectRatioState.AspectRatio_4_3 -> {
                        aspectRatioState = AspectRatioState.AspectRatio_16_9
                        screenAspectRatio = AspectRatio.RATIO_16_9
                        ivFormat.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_crop_16_9_24))
                        startCamera()
                    }
                }
            }
        }
        ivExtension.setOnClickListener {
            if (captureState != CaptureState.VideoState)
                when (extensionState) {
                    ExtensionState.ExtensionStateOn -> {
                        extensionState = ExtensionState.ExtensionStateOff
                        ivExtension.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_blur_off_24))
                        startCamera()
                    }
                    ExtensionState.ExtensionStateOff -> {
                        extensionState = ExtensionState.ExtensionStateOn
                        ivExtension.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_blur_on_24))
                        startCamera()
                    }
                }
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError", "ClickableViewAccessibility")
    private fun startCamera() {

        useCaseGroup = createUseCaseGroup(
            true,
            captureState == CaptureState.PhotoState,
            captureState == CaptureState.VideoState
        )

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup!!)

            // set focus on touch or zoom on pinch
            setFocusOnTouch()
//            setZoomFeature()
        } catch (e: Exception) {
            val msg = "UseCase error: " + e.message
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

    }

    private fun takePhoto() {

        val imageCapture = imageCapture ?: return
        val photoFile = outputDirectory.createImageDateFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    @SuppressLint("RestrictedApi")
    private fun startVideo() {

        val videoFile = outputDirectory.createVideoDateFile()
        val outputFileOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()

        videoCapture?.startRecording(
            outputFileOptions,
            videoExecutor,
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Video saved", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    @SuppressLint("RestrictedApi")
    private fun stopVideo() {
        videoCapture?.stopRecording()
    }

    private fun changeImageCaptureUsecase(flashMode: Int) {
        cameraProvider.unbind(imageCapture)
        imageCapture = createImageCaptureUseCase(
            screenAspectRatio,
            cameraSelector,
            flashMode,
            extensionState == ExtensionState.ExtensionStateOn
        )
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun createUseCaseGroup(
        needPreview: Boolean,
        needPhoto: Boolean,
        needVideo: Boolean
    ): UseCaseGroup {

        val useCaseGroupBuilder = UseCaseGroup.Builder()
        val needExtension = extensionState == ExtensionState.ExtensionStateOn

        preview = createPreviewUseCase(screenAspectRatio, pvCamera, cameraSelector, needExtension)
        if (needPreview)
            useCaseGroupBuilder.addUseCase(preview!!)

        val flashMode = when (flashState) {
            FlashState.FlashStateOn -> ImageCapture.FLASH_MODE_ON
            FlashState.FlashStateOff -> ImageCapture.FLASH_MODE_OFF
            FlashState.FlashStateAuto -> ImageCapture.FLASH_MODE_AUTO
        }
        imageCapture =
            createImageCaptureUseCase(screenAspectRatio, cameraSelector, flashMode, needExtension)
        if (needPhoto)
            useCaseGroupBuilder.addUseCase(imageCapture!!)

        videoCapture = createVideoCaptureUseCase(screenAspectRatio, cameraSelector)
        if (needVideo)
            useCaseGroupBuilder.addUseCase(videoCapture!!)

        return useCaseGroupBuilder.build()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setFocusOnTouch() {
        pvCamera.setOnTouchListener { v, event ->
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    val point = pvCamera.meteringPointFactory.createPoint(event.x, event.y)
                    // auto focus
                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        // auto exposure
                        .addPoint(point, FocusMeteringAction.FLAG_AE)
                        // auto white balance
//                        .addPoint(point, FocusMeteringAction.FLAG_AWB)
                        .setAutoCancelDuration(5, TimeUnit.SECONDS)
                        .build()
                    camera.cameraControl.startFocusAndMetering(action)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setZoomFeature() {

        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio: Float = camera.cameraInfo.zoomState.value?.zoomRatio ?: 0F
                val delta = detector.scaleFactor
                camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }
        val scaleGestureDetector = ScaleGestureDetector(context, listener)

        pvCamera.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    @SuppressLint("RestrictedApi")
    fun checkExtensions() {
        cameraSelector.lensFacing?.let {
            ivExtension.isVisible = isExtensionAvailable(it)
        }
    }

    override fun onDestroyView() {
        if (isVideoStarted) {
            stopVideo()
            isVideoStarted = !isVideoStarted
        }

        preview = null
        imageCapture = null
        videoCapture = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        videoExecutor.shutdown()
    }
}