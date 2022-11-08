package com.example.amazingcameraxapp.presentation.analyzer_camera

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.client.alko.common.mlvision.common.VisionImageProcessor
import com.example.amazingcameraxapp.R
import com.example.amazingcameraxapp.data.*
import kotlinx.android.synthetic.main.analyzer_camera_fragment.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AnalyzerCameraFragment : Fragment() {

    private val TAG = "MlkitCameraFragment"
    private lateinit var viewModel: AnalyzerCameraViewModel

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private var preview: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null

    private var screenAspectRatio: Int = AspectRatio.RATIO_16_9
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraSelectorState: CameraSelectorState = CameraSelectorState.BackCameraState
    private var analyzerState = AnalyzerState.CustomAnalyzerState
    private var useCaseGroup: UseCaseGroup? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.analyzer_camera_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider.AndroidViewModelFactory
            .getInstance(requireActivity().application)
            .create(AnalyzerCameraViewModel::class.java)

        viewModel.cameraProviderLiveData.observe(requireActivity()) {
            cameraProvider = it
            startCamera()
            cvControl.isVisible = true
        }

        ivSwitchCamera.setOnClickListener {
            // todo switch
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
            startCamera()
        }
        rbCustom.setOnClickListener {
            analyzerState = AnalyzerState.CustomAnalyzerState
            startCamera()
        }
        rbFace.setOnClickListener {
            analyzerState = AnalyzerState.FaceAnalyzerState
            startCamera()
        }
        rbPose.setOnClickListener {
            analyzerState = AnalyzerState.PoseAnalyzerState
            // startCamera()
        }
        rbFaceAndPose.setOnClickListener {
            analyzerState = AnalyzerState.FaceAndPoseAnalyzerState
            // startCamera()
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError", "ClickableViewAccessibility")
    private fun startCamera() {

        try {
            val useCaseGroupBuilder = UseCaseGroup.Builder()
            preview = createPreviewUseCase(screenAspectRatio, pvCamera, cameraSelector)
            useCaseGroupBuilder.addUseCase(preview!!)
            createAnalysisUseCase()
            useCaseGroupBuilder.addUseCase(analysisUseCase!!)

            useCaseGroup = useCaseGroupBuilder.build()
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup!!)

        } catch (e: Exception) {
            val msg = "UseCase error: " + e.message
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
        graphicOverlay.clear()
        graphicOverlay.invalidate()
    }

    fun createAnalysisUseCase() {

        graphicOverlay.clear()
        graphicOverlay.invalidate()
        imageProcessor?.stop()
        analysisUseCase?.clearAnalyzer()
        tvInfo.isVisible = analyzerState == AnalyzerState.CustomAnalyzerState
        when (analyzerState) {
            AnalyzerState.CustomAnalyzerState -> {
                analysisUseCase = createLumaAnalysisUseCase(requireContext(), screenAspectRatio) {
                    tvInfo.text = "Avarage luminosity: " + it.toString()
                    Log.d(TAG, "Average luminosity: $it")
                }

            }
            AnalyzerState.FaceAnalyzerState -> {
                imageProcessor = createFaceDetectorProcessor(requireContext()) {
                    // todo do something with list of faces
                }
                analysisUseCase = createFaceDetectorUseCase(
                    requireContext(),
                    screenAspectRatio,
                    cameraSelector,
                    imageProcessor!!,
                    graphicOverlay
                )
            }
            AnalyzerState.PoseAnalyzerState -> {

            }
            AnalyzerState.FaceAndPoseAnalyzerState -> {

            }
        }
    }

    override fun onDestroyView() {
        preview = null
        analysisUseCase = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}