package com.banana.appwithgeolocation.utils

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat

fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.createSimpleDialog(
    title: String,
    view: View,
    textPositive: String,
    textNegative: String
) = AlertDialog.Builder(this).apply {
    setTitle(title)
    setView(view)
    setPositiveButton(textPositive, null)
    setNegativeButton(textNegative, null)
}.create()

fun String.permissionIsGranted(context: Context): Boolean
        = ActivityCompat.checkSelfPermission(context, this) == PackageManager.PERMISSION_GRANTED