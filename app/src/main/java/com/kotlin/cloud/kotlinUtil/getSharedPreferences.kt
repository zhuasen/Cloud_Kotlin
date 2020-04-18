package com.kotlin.cloud.kotlinUtil

import android.content.Context
import android.content.SharedPreferences

//简化获取本地SP的写法
fun getMySharePref(): SharedPreferences = getContext().getSharedPreferences("user_info", Context.MODE_PRIVATE)

fun getInfoString(stringKey : String ) : String = getMySharePref().getString(stringKey,"").toString()

fun getInfoBoolean(booleanKey : String )  = getMySharePref().getBoolean(booleanKey,false)

