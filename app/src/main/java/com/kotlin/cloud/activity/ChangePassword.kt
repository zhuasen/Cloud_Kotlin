package com.kotlin.cloud.activity

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.kotlin.cloud.R
import com.kotlin.cloud.kotlinUtil.getInfoString
import com.kotlin.cloud.oss.UserAsyncTask
import com.rey.material.widget.EditText
import com.tt.whorlviewlibrary.WhorlView
import kotlinx.android.synthetic.main.change_password.*

class ChangePassword :BaseActivity(){

    companion object{
        private var isShowOldPassword = false
        private var isShowNewPassword = false
        private var isShowAssurePassword = false

        var account : String? = null
        lateinit var oldPassword :String
        lateinit var newPassword :String
        lateinit var assurePassword :String

        lateinit var oldPassword_text: EditText
        lateinit var newPassword_text: EditText
        lateinit var assurePassword_text: EditText

        var ChangePasswordProgressBar:WhorlView ?= null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password)

        account = getInfoString("account")

        ChangePasswordProgressBar = findViewById(R.id.changePasswordProgressBar)
        oldPassword_text = findViewById(R.id.oldPasswordInChange_EditText)
        newPassword_text = findViewById(R.id.newPasswordInChange_EditText)
        assurePassword_text = findViewById(R.id.assurePasswordInChange_EditText)
        //确定修改密码的按钮
        sureForChangePassword_Button.setOnClickListener {
            oldPassword = oldPassword_text.text.toString()
            newPassword = newPassword_text.text.toString()
            assurePassword = assurePassword_text.text.toString()
            if(oldPassword.isEmpty() || newPassword.isEmpty()|| assurePassword.isEmpty()){
                if(assurePassword.isEmpty()) assurePasswordInChange_EditText.error = "原密码不能为空"
                else assurePasswordInChange_EditText.clearError()
                if(oldPassword.isEmpty()) oldPasswordInChange_EditText.error = "密码不能为空"
                else oldPasswordInChange_EditText.clearError()
                if(newPassword.isEmpty()) newPasswordInChange_EditText.error = "新密码不能为空"
                else newPasswordInChange_EditText.clearError()
            }else if(newPassword != assurePassword){
                newPasswordInChange_EditText.error = "两次密码不一致"
                assurePasswordInChange_EditText.error = "两次密码不一致"
                oldPasswordInChange_EditText.clearError()
            }else if(oldPassword == newPassword){
                newPasswordInChange_EditText.error = "新密码和原密码不能相同"
                oldPasswordInChange_EditText.error = "新密码和原密码不能相同"
                assurePasswordInChange_EditText.clearError()
            }else{
                changePasswordProgressBar.apply {
                    start()
                    changePasswordProgressBar.visibility = View.VISIBLE
                }
                UserAsyncTask(UserAsyncTask.CHANGE_PASSWORD_TASK).execute()
            }
        }
        //显示密码的三个按钮
        showOldPasswordInChange_Button.setOnClickListener{
            if (!isShowOldPassword) {
                showOldPasswordInChange_Button.background = getDrawable(R.drawable.show)
                oldPasswordInChange_EditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                isShowOldPassword = true
            } else {
                showOldPasswordInChange_Button.background = getDrawable(R.drawable.hide)
                oldPasswordInChange_EditText.transformationMethod = PasswordTransformationMethod.getInstance()
                isShowOldPassword = false
            }
        }
        showNewPasswordInChange_Button.setOnClickListener{
            if (!isShowNewPassword) {
                showNewPasswordInChange_Button.background = getDrawable(R.drawable.show)
                newPasswordInChange_EditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                isShowNewPassword = true
            } else {
                showNewPasswordInChange_Button.background = getDrawable(R.drawable.hide)
                newPasswordInChange_EditText.transformationMethod = PasswordTransformationMethod.getInstance()
                isShowNewPassword = false
            }
        }
        showAssurePasswordInChange_Button.setOnClickListener{
            if (!isShowAssurePassword) {
                showAssurePasswordInChange_Button.background = getDrawable(R.drawable.show)
                assurePasswordInChange_EditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                isShowAssurePassword = true
            } else {
                showAssurePasswordInChange_Button.background = getDrawable(R.drawable.hide)
                assurePasswordInChange_EditText.transformationMethod = PasswordTransformationMethod.getInstance()
                isShowAssurePassword = false
            }
        }
    }
}
