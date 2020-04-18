package com.kotlin.cloud.kotlinUtil

import android.content.Context
import android.content.Intent
import com.kotlin.cloud.oss.OssApplication

/**startActivity用法
 *
 * 在activity中使用
 * startActivity<你要打开的activity>(this@现在的activity)
 *
 * */
inline fun <reified T> startActivity(context:Context){
    val intent = Intent(context,T::class.java)
    context.startActivity(intent)
}

/**startAction用法
 *
 * 在不是activity类中使用
 * startAction<你要打开的activity>()
 *
 * */
inline fun <reified T> startAction(){
    val intent = Intent(getContext(),T::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    getContext().startActivity(intent)
}