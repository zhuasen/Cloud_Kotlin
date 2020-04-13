package com.kotlin.cloudkotlin.kotlinUtil

import android.widget.Toast
import com.kotlin.cloudkotlin.oss.OssApplication

/**
 * Toast工具类
 * 用法：
 * 某个String类型.toast即可,默认短时长
 * 如果要尝试发送Toast，可以传入Toast.LENGTH_LONG
 *
 * */
fun String.toastShort(){
    Toast.makeText(OssApplication.context,this,Toast.LENGTH_SHORT).show();
}

fun String.toastSLong(){
    Toast.makeText(OssApplication.context,this,Toast.LENGTH_LONG).show();
}