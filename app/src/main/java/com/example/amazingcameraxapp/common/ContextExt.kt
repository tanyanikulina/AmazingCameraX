package com.example.amazingcameraxapp.common

import android.content.Context
import com.example.amazingcameraxapp.R
import java.io.File


fun Context.getOutputImageDirectory(): File {
    val mediaDir = externalMediaDirs?.firstOrNull()?.let {
        File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists())
        mediaDir else filesDir
}