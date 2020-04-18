package com.kotlin.cloud.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import cn.pedant.SweetAlert.SweetAlertDialog
import com.kotlin.cloud.R
import com.kotlin.cloud.kotlinUtil.*
import com.kotlin.cloud.oss.ImageAsyncTask
import com.kotlin.cloud.oss.UserAsyncTask
import com.kotlin.cloud.usefulClass.GlideImageEngine
import com.kotlin.cloud.usefulClass.imageUtil
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import kotlinx.android.synthetic.main.main_layout.*

/**
 * 主页面，功能：
 * 1、上传备份图片
 * 2、进入选择图片下载回本地页面
 * 3、修改密码
 * 4、注销账户
 *
 * 不足:
 * 1、功能1会占用大量内存空间，显示在app占用内存中，可以监测并自动清理？
 * */
class MainActivity : BaseActivity() {

    companion object{
        //注销账号的警告框
        var deleteAccountDialog: SweetAlertDialog? = null

        //选择图片上传的进度提示框
        var uploadProgressDialog: SweetAlertDialog? = null

        //用户选择图片上传的图片uri
        var imgPathForUploadList  = ArrayList<String>()

        lateinit var account:String
        //图片选择器的requestCode
        private const val selectRequestCode = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        //获取账号
        account = getInfoString("account")
        //欢迎文字
        welcome_TextView.text =  resources.getString(R.string.welcomeWord,account)

        //修改密码按钮
        changePasswordInMain_Button.setOnClickListener {
            startActivity<ChangePassword>(this@MainActivity)
        }
        //注销账户按钮
        deleteAccount_Button.setOnClickListener {
            if(account == "t") "勿删t号".toastLong()
            else{
                deleteAccountDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE).apply {
                    titleText= "注销账号确认"
                    contentText = "您真的要注销账号吗"
                    confirmText = "是的"
                    cancelText = "取消"
                    setConfirmClickListener {
                        titleText = "删除账号中"
                        contentText = ""
                        showCancelButton(false)
                        setCancelable(false)
                        changeAlertType(SweetAlertDialog.PROGRESS_TYPE)
                        UserAsyncTask(UserAsyncTask.DELETE_ACCOUNT_TASK).execute()
                    }
                    setCancelClickListener {
                        it.dismissWithAnimation()
                        "已取消".toastShort()
                    }
                    show()
                }
            }
        }
        //上传图片按钮
        choosePictureForUpload_Button.setOnClickListener {
            Matisse.from(this) //显示所有图片和视频
                .choose(MimeType.ofAll(), false)
                .countable(true)
                .maxSelectable(100) // 图片选择的最多数量
                .gridExpectedSize(resources.getDimensionPixelSize(R.dimen.grid_expected_size))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f) // 缩略图的比例
                .imageEngine(GlideImageEngine()) // 使用的图片加载引擎
                .forResult(MainActivity.selectRequestCode) // 设置作为标记的请求码
            //在onActivityResult中处理
        }
        //下载图片按钮
        downloadPictureFromCloud_Button.setOnClickListener {
            startActivity<SelectPicture>(this@MainActivity)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            when(requestCode){
                selectRequestCode->{
                    // 图库选择图片
                    uploadProgressDialog = SweetAlertDialog(this@MainActivity, SweetAlertDialog.PROGRESS_TYPE).apply {
                        titleText = "正在上传图片,请耐心等待"
                        progressHelper.barColor = Color.parseColor("#A5DC86")
                        show()
                    }
                    val imageListUri = Matisse.obtainResult(data)
                    //imagePathList存放真实路径

                    var imageType: String
                    for (uri in imageListUri) {
                        //调用图片工具类解析得到真实的Uri,先判断是否为图片文件split("\\.")[1]
                        imageType = imageUtil.getImagePathFromUri(this, uri).getStrByIndex('.',1)
                        if (imageType == "jpg" || imageType == "jpeg" || imageType == "png") {
                            imgPathForUploadList.add(imageUtil.getImagePathFromUri(this, uri))
                        }
                        else "当前仅能上传图片文件,已自动过滤掉视频文件".toastShort()
                    }
                    //选择不为空，则通过自己的异步接口上传图片
                    if (imgPathForUploadList.isNotEmpty()) {
                        ImageAsyncTask(ImageAsyncTask.UPLOAD_FILE_TASK).execute()
                    }
                    else  {
                        "未上传任何文件".toastShort()
                        uploadProgressDialog?.dismissWithAnimation()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
       UserAsyncTask(UserAsyncTask.CLEAR_MAIN_CACHE).execute()
    }
}
