package com.example.amazingcameraxapp.common

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

const val DATE_FILENAME_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS"

@SuppressLint("ConstantLocale")
val DEFAULT_LOCALE: Locale = Locale.getDefault()

fun Date.formatDate(
    format: String = DATE_FILENAME_FORMAT,
    locale: Locale = DEFAULT_LOCALE
): String {
    return SimpleDateFormat(format, locale).format(this)
}

fun Date.todayFormatted(
    format: String = DATE_FILENAME_FORMAT,
    locale: Locale = DEFAULT_LOCALE
): String {
    return formatDate(format, locale)
}