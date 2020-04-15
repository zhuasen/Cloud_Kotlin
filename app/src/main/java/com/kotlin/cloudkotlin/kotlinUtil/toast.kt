package com.kotlin.cloudkotlin.kotlinUtil

import android.widget.Toast
import com.kotlin.cloudkotlin.oss.OssApplication

/**
 * Toast工具类
 * 用法：
 * 某个String类型.toastShort即可,也可使用长时toast
 *
 * */
fun String.toastShort(){
    Toast.makeText(OssApplication.Context,this,Toast.LENGTH_SHORT).show();
}

fun String.toastSLong(){
    Toast.makeText(OssApplication.Context,this,Toast.LENGTH_LONG).show();
}