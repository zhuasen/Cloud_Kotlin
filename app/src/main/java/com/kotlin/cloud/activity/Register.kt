package com.kotlin.cloud.activity

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.kotlin.cloud.R
import com.kotlin.cloud.oss.UserAsyncTask
import com.rey.material.widget.EditText
import com.tt.whorlviewlibrary.WhorlView
import kotlinx.android.synthetic.main.register_layout.*

//注册活动，检查账号密码的合法性
class Register : BaseActivity(){

    companion object{
        lateinit var  account: String
        lateinit var password: String
        lateinit var assurePassword: String
        lateinit var assurePassword_text: EditText
        lateinit var account_text: EditText
        lateinit var password_text: EditText
        var RegisterProgressBar: WhorlView? = null

        private var isShowPassword = false
        private var isShowAssurePassword = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_layout)

        account_text = findViewById(R.id.accountInRegister_EditText)
        password_text = findViewById(R.id.passwordInRegister_EditText)
        assurePassword_text = findViewById(R.id.assurePasswordInRegister_EditText)
        RegisterProgressBar = findViewById(R.id.registerProgressBar)

        //点击注册按钮
        registerInRegister_Button.setOnClickListener {

            account = account_text.text.toString()
            password = password_text.text.toString()
            assurePassword = assurePassword_text.text.toString()
            /**
             * 这里检查
             * 1、三行是否为空
             * 2、密码不一致
             * 3、账号符合格式
             *
             * 在异步任务检查
             *       账号是否已被注册
             * */
            if(account.isEmpty() || password.isEmpty()|| assurePassword.isEmpty()){
                if(assurePassword.isEmpty()) assurePasswordInRegister_EditText.error = "密码不能为空"
                else assurePasswordInRegister_EditText.clearError()
                if(password.isEmpty()) passwordInRegister_EditText.error = "密码不能为空"
                else passwordInRegister_EditText.clearError()
                if(account.isEmpty()) accountInRegister_EditText.error = "账号不能为空"
                else accountInRegister_EditText.clearError()
            }else if(password != assurePassword){
                passwordInRegister_EditText.error = "两次密码不一致"
                assurePasswordInRegister_EditText.error = "两次密码不一致"
                accountInRegister_EditText.clearError()
            }else if (account.contains('\n')){
                accountInRegister_EditText.error = "用户名不能包含换行符"
                passwordInRegister_EditText.clearError()
                assurePasswordInRegister_EditText.clearError()
            }else if(account.length > 19){
                accountInRegister_EditText.error = "用户名过长"
                passwordInRegister_EditText.clearError()
                assurePasswordInRegister_EditText.clearError()
            }else if(account.startsWith('-')|| account.endsWith('-')){
                accountInRegister_EditText.error = "用户名不能以‘-’开头或结尾"
                passwordInRegister_EditText.clearError()
                assurePasswordInRegister_EditText.clearError()
            }else {
                for(ch in account) {
                    if (ch in '0'..'9' || ch in 'a'..'z' || ch == '-') continue
                    account_text.error = "用户名只能由小写字母，数字，“-”组成"
                    return@setOnClickListener
                }
                registerProgressBar.apply {
                    start()
                    visibility = View.VISIBLE
                }
                UserAsyncTask(UserAsyncTask.REGISTER_TASK).execute()
            }
        }
        //显示密码的两个按钮
        showPasswordInRegister_Button.setOnClickListener{
            if (!isShowPassword) {
                showPasswordInRegister_Button.background = getDrawable(R.drawable.show)
                password_text.transformationMethod = HideReturnsTransformationMethod.getInstance()
                isShowPassword = true
            } else {
                showPasswordInRegister_Button.background = getDrawable(R.drawable.hide)
                password_text.transformationMethod = PasswordTransformationMethod.getInstance()
                isShowPassword = false
            }
        }
        showAssurePasswordInRegister_Button.setOnClickListener {
            if (!isShowAssurePassword) {
                showAssurePasswordInRegister_Button.background = getDrawable(R.drawable.show)
                assurePassword_text.transformationMethod = HideReturnsTransformationMethod.getInstance()
                isShowAssurePassword = true
            } else {
                showAssurePasswordInRegister_Button.background = getDrawable(R.drawable.hide)
                assurePassword_text.transformationMethod = PasswordTransformationMethod.getInstance()
                isShowAssurePassword = false
            }
        }
    }
}
