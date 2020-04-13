package com.kotlin.cloudkotlin.activity

import android.os.Bundle
import com.kotlin.cloudkotlin.R

class Login : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_layout)
    }
}
