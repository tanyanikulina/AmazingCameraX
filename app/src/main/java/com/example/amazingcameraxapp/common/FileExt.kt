package com.example.amazingcameraxapp.common

import java.io.File
import java.util.*

const val FILENAME_JPG = ".jpg"
const val FILENAME_VIDEO = ".mp4"

fun File.createVideoDateFile(): File {
    return File(this, Date().todayFormatted(DATE_FILENAME_FORMAT) + FILENAME_VIDEO)
}

fun File.createImageDateFile(): File {
    return File(this, Date().todayFormatted(DATE_FILENAME_FORMAT) + FILENAME_JPG)
}