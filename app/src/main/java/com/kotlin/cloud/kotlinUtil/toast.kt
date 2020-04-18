package com.kotlin.cloud.kotlinUtil

import android.widget.Toast
import com.kotlin.cloud.oss.OssApplication

/**
 * Toast工具类
 * 用法：
 * 某个String类型.toastShort即可,也可使用长时toast
 *
 * */
fun String.toastShort(){
    Toast.makeText(getContext(),this,Toast.LENGTH_SHORT).show();
}

fun String.toastLong(){
    Toast.makeText(getContext(),this,Toast.LENGTH_LONG).show();
}