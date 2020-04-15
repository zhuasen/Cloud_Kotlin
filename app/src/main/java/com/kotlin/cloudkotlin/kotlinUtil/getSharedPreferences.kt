package com.kotlin.cloudkotlin.kotlinUtil

import android.content.Context
import android.content.SharedPreferences
import com.kotlin.cloudkotlin.oss.OssApplication

//简化获取本地SP的写法
fun getMySharePref(): SharedPreferences = OssApplication.Context.getSharedPreferences("user_info", Context.MODE_PRIVATE)