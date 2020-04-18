package com.kotlin.cloud.kotlinUtil

import android.graphics.drawable.Drawable
import com.kotlin.cloud.oss.OssApplication

fun getColor(colorResource : Int):Int = getContext().getColor(colorResource)

fun getDrawable(drawableResource : Int): Drawable? = getContext().getDrawable(drawableResource)
