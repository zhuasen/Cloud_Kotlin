package com.kotlin.cloud.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kotlin.cloud.usefulClass.ActivityCollector

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCollector.addActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityCollector.removeActivity(this)
    }
}
