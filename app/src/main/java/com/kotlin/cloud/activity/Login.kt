package com.kotlin.cloud.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.core.content.edit
import cn.pedant.SweetAlert.SweetAlertDialog
import com.kotlin.cloud.R
import com.kotlin.cloud.activity.SelectPicture.Companion.downloadStart
import com.kotlin.cloud.kotlinUtil.*
import com.kotlin.cloud.oss.OssInformation
import com.kotlin.cloud.oss.UserAsyncTask
import com.kotlin.cloud.usefulClass.ActivityCollector.exitApp
import com.kotlin.cloud.usefulClass.permissionsUtil
import com.rey.material.widget.EditText
import com.tt.whorlviewlibrary.WhorlView
import kotlinx.android.synthetic.main.login_layout.*
import java.io.File

class Login : BaseActivity() {

    //提供给异步任务使用的变量
    companion object{
        lateinit var account:String
        lateinit var password: String
        lateinit var account_text: EditText
        lateinit var password_text: EditText
        var LoginProgressBar: WhorlView? = null

        //动态权限申请列表
        private val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        //布尔值记录是否显示密码
        private var isShowPassword = false

        private val compressPictureFile = File(OssInformation.downloadCompressDirectory)
        private val originalPictureFile = File(OssInformation.downloadOriginalDirectory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_layout)
        account_text = findViewById(R.id.accountInLogin_EditText)
        password_text = findViewById(R.id.passwordInLogin_EditText)

        //如果记住了密码就直接显示
        if (getMySharePref().contains("remember_password")&& getInfoBoolean("remember_password")) {
            account_text.setText(getInfoString("account"))
            password_text.setText(getInfoString("password"))
            rememberPasswordInLogin_CheckBox.isChecked = true
        }

        //一开始就申请权限创建文件夹，否则无法运行（巨大bug...）
        val permissionsResult = object : permissionsUtil.IPermissionsResult{
            //通过后干的事：创建文件夹
            override fun passPermission() {
                //存缓存图片的，一定要新建有这个文件夹，极重要
                compressPictureFile.mkdirs()
                originalPictureFile.mkdirs()
            }

            override fun forbidPermission() {
                SweetAlertDialog(this@Login, SweetAlertDialog.WARNING_TYPE).apply {
                    titleText = "权限被禁用了"
                    contentText = "禁用权限将导致无法使用本应用"
                    confirmText = "手动设置"
                    cancelText = "退出应用"
                    //点击手动设置按钮
                    setConfirmClickListener {
                        it.dismissWithAnimation()
                        "请手动设置存储空间的权限".toastShort()
                        //打开系统设置
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${this@Login.packageName}"))
                        startActivity(intent)
                    }
                    //点击退出按钮
                    setCancelClickListener {
                        "请卸载app".toastShort()
                        it.dismissWithAnimation()
                        exitApp()
                    }
                    setCancelable(false)
                    show()
                }
            }
        }
        permissionsUtil.getInstance().checkPermissions(this, permissions, permissionsResult)

        //登陆按钮：
        loginInLogin_Button.setOnClickListener {
            //如果没有这两个文件夹就不能登陆
            if (!compressPictureFile.exists() || !originalPictureFile.exists())
                permissionsUtil.getInstance().checkPermissions(this, permissions, permissionsResult)
            else {
                account = account_text.text.toString()
                password = password_text.text.toString()

                //如果账号或者密码为空,或者账号包含换行符
                if (password.isEmpty() || account.isEmpty()) {
                    if (password.isEmpty()) passwordInLogin_EditText.error = "密码不能为空"
                    else passwordInLogin_EditText.clearError()
                    if (account.isEmpty()) accountInLogin_EditText.error = "账号不能为空"
                    else accountInLogin_EditText.clearError()
                } else if (account.contains('\n')) {
                    accountInLogin_EditText.error = "用户名不存在，请先去注册"
                    passwordInLogin_EditText.clearError()
                } else if (downloadStart != 0 && account != getInfoString("account")) {
                    SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).apply {
                        titleText = "请勿一次登录不同的账号"
                        contentText = "可以选择退出app后重新登陆，或者不再登录当前账号"
                        cancelText = "取消"
                        confirmText = "退出"
                        setCancelClickListener {
                            "已取消登陆".toastShort()
                            it.dismissWithAnimation()
                        }
                        setConfirmClickListener {
                            "重新打开app即可".toastShort()
                            it.dismissWithAnimation()
                            exitApp()
                        }
                        show()
                    }
                }
                //上述错误都没有犯的情况下
                else {
                    //是否记住密码,根据复选框的选择来确定下次打开时是否记住密码
                    getMySharePref().edit {
                        if (rememberPasswordInLogin_CheckBox.isChecked) putBoolean(
                            "remember_password",
                            true
                        )
                        else putBoolean("remember_password", false)
                    }

                    //加载框
                    LoginProgressBar = findViewById(R.id.loginProgressBar)
                    loginProgressBar.apply {
                        start()
                        visibility = View.VISIBLE
                    }

                    //注册的任务
                    UserAsyncTask(UserAsyncTask.LOGIN_TASK).execute()
                }
            }
        }
        //注册按钮
        registerInLogin_Button.setOnClickListener{
                if(!compressPictureFile.exists()||!originalPictureFile.exists())
                    permissionsUtil.getInstance().checkPermissions(this, permissions, permissionsResult)
                else
                    startActivity<Register>(this@Login)
            }
        //显示密码按钮
        showPasswordInLogin_Button.setOnClickListener {
                //如果现在没有显示，就显示
                if (!isShowPassword) {
                    showPasswordInLogin_Button.background = getDrawable(R.drawable.show)
                    password_text.transformationMethod = HideReturnsTransformationMethod.getInstance()
                    isShowPassword = true
                } else {
                    showPasswordInLogin_Button.background = getDrawable(R.drawable.hide)
                    password_text.transformationMethod = PasswordTransformationMethod.getInstance()
                    isShowPassword = false
                }
            }


    }

    //申请权限返回结果后调用PermissionUtil工具类进行处理
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsUtil.getInstance().onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }
}


