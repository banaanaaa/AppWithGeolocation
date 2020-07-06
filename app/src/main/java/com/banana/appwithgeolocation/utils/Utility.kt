package com.banana.appwithgeolocation.utils

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.core.view.setMargins
import com.banana.appwithgeolocation.R
import java.util.jar.Attributes

fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.createSimpleDialog(
    title: String,
    view: View,
    textPositive: String,
    textNegative: String
): AlertDialog = AlertDialog.Builder(this).apply {
    setTitle(title)
    setView(view)
    setPositiveButton(textPositive, null)
    setNegativeButton(textNegative, null)
}.create()

fun String.permissionIsGranted(context: Context): Boolean
        = ActivityCompat.checkSelfPermission(context, this) == PackageManager.PERMISSION_GRANTED

fun Context.createClearLayout(description: String): LinearLayout = LinearLayout(this).apply {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { gravity = Gravity.CENTER }
    addView(TextView(
        this@createClearLayout,
        null,
        0,
        R.style.TextViewStyleA
    ).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                resources.getDimension(R.dimen.spacing_normal).toInt(),
                resources.getDimension(R.dimen.spacing_small).toInt(),
                resources.getDimension(R.dimen.spacing_normal).toInt(),
                resources.getDimension(R.dimen.spacing_small).toInt()
            )
        }
        text = description
    })
}

