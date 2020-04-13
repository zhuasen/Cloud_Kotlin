package com.kotlin.cloudkotlin.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kotlin.cloudkotlin.usefulClass.ActivityCollector

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
