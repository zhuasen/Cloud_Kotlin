package com.kotlin.cloud.kotlinUtil

import android.util.Log

//简单打印方法
fun <T> T.logI(){
    Log.i("测试Log",this.toString())
}

fun <T> T.logE(){
    Log.e("测试Log",this.toString())
}

fun <T> T.logD(){
    Log.d("测试Log",this.toString())
}