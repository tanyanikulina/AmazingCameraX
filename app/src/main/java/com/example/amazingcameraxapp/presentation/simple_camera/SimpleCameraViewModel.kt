package com.example.amazingcameraxapp.presentation.simple_camera

import android.app.Application
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class SimpleCameraViewModel(val app: Application) : AndroidViewModel(app) {

    val cameraProviderLiveData = MutableLiveData<ProcessCameraProvider>()

    init {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(app)
        cameraProviderFuture.addListener(
            Runnable {
                cameraProviderFuture.get()?.let {
                    cameraProviderLiveData.postValue(it)
                }
            },
            ContextCompat.getMainExecutor(app)
        )
    }

}