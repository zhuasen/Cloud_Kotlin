
package com.kotlin.cloud.oss

import android.os.AsyncTask
import android.os.Environment
import android.view.View
import androidx.core.content.edit
import cn.pedant.SweetAlert.SweetAlertDialog
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.model.*
import com.kotlin.cloud.activity.*
import com.kotlin.cloud.activity.SelectPicture.Companion.downloadEnd
import com.kotlin.cloud.activity.SelectPicture.Companion.downloadStart
import com.kotlin.cloud.activity.SelectPicture.Companion.imageForSelectAdapter
import com.kotlin.cloud.kotlinUtil.getContext
import com.kotlin.cloud.kotlinUtil.getMySharePref
import com.kotlin.cloud.kotlinUtil.startAction
import com.kotlin.cloud.kotlinUtil.toastShort
import java.io.File
import java.io.IOException

/**
 * 异步任务分成两部分好管理
 * 这是第一部分，不涉及android 10 作用域存储
 * 内容有
 * 1、登陆
 * 2、注册
 * 3、注销账户
 * 4、修改密码
 * 5、清理垃圾
 *
 */

//传入任务类型
@Suppress("DEPRECATION")
class UserAsyncTask (private val taskType: Int) : AsyncTask<Unit, Unit, Int>() {

    //异步任务主体：根据特定指令（taskType）完成各项任务
    override fun doInBackground(vararg params: Unit): Int {
        when (taskType) {
            //登录
            LOGIN_TASK->{
                try {
                    //先检查用户名是否存在
                    if (!oss.doesObjectExist("user-information", Login.account)) return LOGIN_ERROR_ACCOUNT
                    //获取密码对比
                    val getPasswordRequest = GetObjectRequest("user-information", Login.account)
                    val getPasswordResult: GetObjectResult = oss.getObject(getPasswordRequest)
                    val inputStream = getPasswordResult.objectContent
                    val buffer = ByteArray(2048)
                    while (inputStream.read(buffer) != -1) {
                        //trim除去乱码，returnPassword保存account对应的密码
                        val returnPassword = String(buffer).trim { it <= ' ' }
                        return if (returnPassword == Login.password) LOGIN_SUCCESS
                         else LOGIN_ERROR_PASSWORD
                    }
                }catch (e: ClientException) {
                    return LOGIN_NETWORK_FAIL
                } catch (e: ServiceException) {
                    return LOGIN_SERVICE_FAIL
                }catch (e: IOException) {
                    return LOGIN_IOE_ERROR
                }
            }
            //注册
            REGISTER_TASK->{
                try {
                    //1、检查这个account有无被注册过
                    if (oss.doesObjectExist("user-information", Register.account)) return REGISTER_DUPLICATION_ACCOUNT
                    //2、上传用户资料：账号密码
                    val passwordByte: ByteArray = Register.password.toByteArray()
                    //3、将密码转换为byte数组上传
                    val putUserInfoRequest = PutObjectRequest("user-information", Register.account, passwordByte)
                    oss.putObject(putUserInfoRequest)
                    //4、创建存储空间
                    val createBucketRequest = CreateBucketRequest(Register.account + randomCode).apply {
                        bucketACL = CannedAccessControlList.Private
                        locationConstraint = "oss-cn-shenzhen"
                        bucketStorageClass = StorageClass.Standard
                    }
                    oss.createBucket(createBucketRequest)
                } catch (e: ClientException) {
                    return REGISTER_NETWORK_FAIL
                } catch (e: ServiceException) {
                    return REGISTER_SERVICE_FAIL
                }
                return REGISTER_SUCCESS
            }
            //修改密码
            CHANGE_PASSWORD_TASK->{
                try {
                    //1、获取密码对比
                    val getPasswordRequest = GetObjectRequest("user-information", ChangePassword.account)
                    val getPasswordResult: GetObjectResult = oss.getObject(getPasswordRequest)
                    val inputStream = getPasswordResult.objectContent
                    val buffer = ByteArray(2048)
                    while (inputStream.read(buffer) != -1) {
                        //trim除去乱码，returnPassword保存account对应的密码
                        val returnPassword = String(buffer).trim { it <= ' ' }
                        if (returnPassword != ChangePassword.oldPassword) return CHANGE_WRONG_PASSWORD_FAIL }
                    //2、删除账号信息(在"user-information"中存有的密码账号)
                    val deleteAccountRequest = DeleteObjectRequest("user-information", ChangePassword.account)
                    oss.deleteObject(deleteAccountRequest)
                    //3、上传用户新的资料：账号密码。将密码转换为byte数组
                    val passwordByte: ByteArray = ChangePassword.newPassword.toByteArray()
                    val putUserInfoRequest = PutObjectRequest("user-information", ChangePassword.account, passwordByte)
                    oss.putObject(putUserInfoRequest)
                }catch (e: IOException) {
                    return CHANGE_PASSWORD_IOE_FAIL
                } catch (e: ClientException) {
                    return CHANGE_PASSWORD_NETWORK_FAIL
                } catch (e: ServiceException) {
                    return CHANGE_PASSWORD_SERVICE_FAIL
                }
                return CHANGE_PASSWORD_SUCCESS
            }
            //注销账号
            DELETE_ACCOUNT_TASK->{
                try {
                    //1、获取object数，先判断是不是空
                    if (!oss.doesObjectExist(MainActivity.account + randomCode, "1")) {
                        objectNumberInTotal = 0
                    } else {
                        var maxNumber = 2000
                        var targetNumber: Int
                        var minNumber = 1
                        while (minNumber <= maxNumber) {
                            targetNumber = (minNumber + maxNumber) / 2
                            if (oss.doesObjectExist(MainActivity.account +randomCode, targetNumber.toString())) {
                                minNumber = targetNumber + 1
                                //一个存在但是他的后一个不存在，就是已经找到最后一个了
                                if (!oss.doesObjectExist(MainActivity.account + randomCode, (targetNumber + 1).toString())) {
                                    objectNumberInTotal = targetNumber
                                    break
                                }
                            } else maxNumber = targetNumber - 1
                        }
                    }
                    ///2、删除账号信息(在"user-information"中存有的密码账号)
                    val deleteAccountRequest = DeleteObjectRequest("user-information", MainActivity.account)
                    oss.deleteObject(deleteAccountRequest)
                    ///3、删除账号下图片
                    for (i in 1..objectNumberInTotal) {
                        val deleteFileRequest = DeleteObjectRequest(MainActivity.account + randomCode, i.toString())
                        oss.deleteObject(deleteFileRequest)
                    }
                    ///4、删除bucket
                    val deleteBucketRequest = DeleteBucketRequest(MainActivity.account + randomCode)
                    oss.deleteBucket(deleteBucketRequest)
                } catch (e: ClientException) {
                    return DELETE_NETWORK_FAIL
                } catch (e: ServiceException) {
                    return DELETE_SERVICE_FAIL
                }
                return DELETE_ACCOUNT_SUCCESS
            }
            //主页面退出时清理垃圾
            CLEAR_MAIN_CACHE->{
                clearCache()
                return CLEAR_CACHE_MAIN_SUCCESS
            }
            //登陆时清理垃圾
            CLEAR_LOGIN_CACHE->{
                clearCache()
                return CLEAR_CACHE_LOGIN_SUCCESS
            }
            //注册时清理垃圾
            CLEAR_REGISTER_CACHE->{
                clearCache()
                return CLEAR_CACHE_REGISTER_SUCCESS
            }
            else -> return RETURN_ERROR_TYPE
        }
        return RETURN_ERROR_TYPE
    }

    override fun onPostExecute(result: Int) {
        when(result){
            //登陆成功
            LOGIN_SUCCESS->{
                //将密码存好，以后要用加密算法
                getMySharePref().edit{
                    putString("account", Login.account)
                    putString("password", Login.password)
                }
                //清一下输入栏的错误
                if (Login.account_text.error != null) Login.account_text.clearError()
                if (Login.password_text.error != null) Login.password_text.clearError()
                //清理垃圾
                UserAsyncTask(CLEAR_LOGIN_CACHE).execute()
            }
            //登录失败<-用户输入的登陆账号尚未注册
            LOGIN_ERROR_ACCOUNT->{
                Login.account_text.error = "用户名不存在，请先去注册"
                Login.password_text.clearError()
                Login.LoginProgressBar?.let {
                    it.stop()
                    it.visibility = View.GONE
                }
            }
            //登录失败<-用户输入密码错误
            LOGIN_ERROR_PASSWORD->{
                Login.password_text.error = "密码输入错误"
                Login.account_text.clearError()
                Login.LoginProgressBar?.let {
                    it.stop()
                    it.visibility = View.GONE
                }
            }
            //登录失败<-io流异常
            LOGIN_IOE_ERROR->{
                "输入输出流异常".toastShort()
                Login.account_text.clearError()
                Login.password_text.clearError()
                Login.LoginProgressBar?.let {
                    it.stop()
                    it.visibility = View.GONE
                }
            }
            //登录失败<-网络连接异常
            LOGIN_NETWORK_FAIL->{
                "网络异常".toastShort()
                Login.account_text.clearError()
                Login.password_text.clearError()
                Login.LoginProgressBar?.let {
                    it.stop()
                    it.visibility = View.GONE
                }
            }
            //登录失败<-服务器异常
            LOGIN_SERVICE_FAIL->{
                "服务器异常".toastShort()
                Login.account_text.clearError()
                Login.password_text.clearError()
                Login.LoginProgressBar?.let {
                    it.stop()
                    it.visibility = View.GONE
                }
            }

            //注册成功
            REGISTER_SUCCESS->{
                if (Register.account_text.error != null) Register.account_text.clearError()
                if (Register.password_text.error != null) Register.password_text.clearError()
                if (Register.assurePassword_text.error != null) Register.assurePassword_text.clearError()
                //将密码存好，以后要用加密算法
                getMySharePref().edit{
                    putString("account", Register.account)
                    putString("password", Register.password)
                }
                UserAsyncTask(CLEAR_REGISTER_CACHE).execute()
            }
            //注册失败<-重名
            REGISTER_DUPLICATION_ACCOUNT->{
                Register.account_text.error = "用户名已被注册"
                Register.assurePassword_text.clearError()
                Register.password_text.clearError()
                Register.RegisterProgressBar?.let {
                    it.stop()
                    it.visibility = View.GONE
                }
            }
            //注册失败<-网络连接异常
            REGISTER_NETWORK_FAIL->{
                "网络连接失败".toastShort()
                Register.RegisterProgressBar?.let {
                    it.stop()
                    it.visibility = View.GONE
                }
            }
            //注册失败<-服务器异常
            REGISTER_SERVICE_FAIL->{
                "服务器错误".toastShort()
                Register.RegisterProgressBar?.let {
                    it.stop()
                    it.visibility = View.GONE
                }
            }

            //修改密码成功
            CHANGE_PASSWORD_SUCCESS->{
                //关旋转progressBar
                ChangePassword.ChangePasswordProgressBar?.let{
                    it.visibility = View.GONE
                    it.stop()
                }
                if (ChangePassword.oldPassword_text.error != null) ChangePassword.oldPassword_text.clearError()
                if (ChangePassword.newPassword_text.error != null) ChangePassword.newPassword_text.clearError()
                if (ChangePassword.assurePassword_text.error != null) ChangePassword.assurePassword_text.clearError()
                startAction<Login>()
                "成功修改密码，请重新登陆".toastShort()
            }
            //修改密码失败<-原密码错误
            CHANGE_WRONG_PASSWORD_FAIL->{
                //关旋转progressBar
                ChangePassword.ChangePasswordProgressBar?.let{
                    it.visibility = View.GONE
                    it.stop()
                }
                ChangePassword.oldPassword_text.error = "原密码错误"
                ChangePassword.newPassword_text.clearError()
                ChangePassword.assurePassword_text.clearError()
            }
            //修改密码失败<-网络错误
            CHANGE_PASSWORD_NETWORK_FAIL->{
                //关旋转progressBar
                ChangePassword.ChangePasswordProgressBar?.let{
                    it.visibility = View.GONE
                    it.stop()
                }
                "网络连接异常".toastShort()
                return;
            }
            //修改密码失败<-服务器错误
             CHANGE_PASSWORD_SERVICE_FAIL->{
                //关旋转progressBar
                ChangePassword.ChangePasswordProgressBar?.let{
                    it.visibility = View.GONE
                    it.stop()
                }
                "服务器异常".toastShort()
            }
            //修改密码失败<-io流错误
            CHANGE_PASSWORD_IOE_FAIL->{
                //关旋转progressBar
                ChangePassword.ChangePasswordProgressBar?.let{
                    it.visibility = View.GONE
                    it.stop()
                }
                "输入输出流异常".toastShort()
                return;
            }

            //注销账户成功
            DELETE_ACCOUNT_SUCCESS->{
                //切换对话框，退出app
                MainActivity.deleteAccountDialog?.apply {
                    titleText = "成功注销账号"
                    contentText = "您需重新登录"
                    confirmText = "OK"
                    setConfirmClickListener {
                        startAction<Login>()
                        it.dismissWithAnimation()
                    }
                    changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                    setCancelable(false)
                }

            }
            //注销失败<-网络原因
            DELETE_NETWORK_FAIL->{
                MainActivity.deleteAccountDialog?.apply {
                    titleText = "注销账号失败"
                    contentText = "网络连接失败，请重新连接"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                    setCancelable(false)
                }
            }
            //注销失败<-服务器原因
            DELETE_SERVICE_FAIL->{
                MainActivity.deleteAccountDialog?.apply {
                    titleText = "注销账号失败"
                    contentText = "服务器异常，请重试"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                    setCancelable(false)
                }
            }

            //退出主页面时清理垃圾成功
            //以下最重要的是清空adapter的list
            CLEAR_CACHE_MAIN_SUCCESS->{
                SelectPicture.imagePathForAdapterList.clear()
            }
            //登陆时清理垃圾成功
            CLEAR_CACHE_LOGIN_SUCCESS->{
                "欢迎登陆".toastShort()
                //进入主界面
                startAction<MainActivity>()
                //停止转动
                Login.LoginProgressBar?.let {
                    it.visibility = View.GONE
                    it.stop()
                }
                SelectPicture.imagePathForAdapterList.clear()
            }
            //注册时清理垃圾成功
            CLEAR_CACHE_REGISTER_SUCCESS->{
                "注册成功，欢迎使用".toastShort()
                Register.RegisterProgressBar?.let {
                    it.visibility = View.GONE
                    it.stop()
                }
                //进入主界面
                startAction<MainActivity>()
                SelectPicture.imagePathForAdapterList.clear()
            }

            //错误请求
            RETURN_ERROR_TYPE->{
                "bug".toastShort()
            }
        }
    }

    companion object{

        private const val randomCode = OssInformation.ossRandomCode
        private val oss = OssApplication.oss
        var objectNumberInTotal = 0
        //注册事件：
        const val REGISTER_TASK = 1
        private const val REGISTER_SUCCESS = 2
        private const val REGISTER_DUPLICATION_ACCOUNT = 3
        private const val REGISTER_SERVICE_FAIL = 4
        private const val REGISTER_NETWORK_FAIL = 5

        //登陆事件
        const val LOGIN_TASK = 6
        private const val LOGIN_SUCCESS = 7
        private const val LOGIN_ERROR_ACCOUNT = 8
        private const val LOGIN_ERROR_PASSWORD = 9
        private const val LOGIN_IOE_ERROR = 10
        private const val LOGIN_SERVICE_FAIL = 11
        private const val LOGIN_NETWORK_FAIL = 12

        //修改密码
        const val CHANGE_PASSWORD_TASK = 13
        private const val CHANGE_PASSWORD_SUCCESS = 14
        private const val CHANGE_WRONG_PASSWORD_FAIL = 15
        private const val CHANGE_PASSWORD_NETWORK_FAIL = 16
        private const val CHANGE_PASSWORD_SERVICE_FAIL = 17
        private const val CHANGE_PASSWORD_IOE_FAIL = 18

        //注销账户指令
        const val DELETE_ACCOUNT_TASK = 19
        private const val DELETE_NETWORK_FAIL = 20
        private const val DELETE_SERVICE_FAIL = 21
        private const val DELETE_ACCOUNT_SUCCESS = 22

        //清理垃圾
        const val CLEAR_LOGIN_CACHE = 23
        const val CLEAR_REGISTER_CACHE = 24
        const val CLEAR_MAIN_CACHE = 25
        private const val CLEAR_CACHE_LOGIN_SUCCESS = 26
        private const val CLEAR_CACHE_REGISTER_SUCCESS = 27
        private const val CLEAR_CACHE_MAIN_SUCCESS = 28

        //不符合要求的taskType
        private const val RETURN_ERROR_TYPE = 100
    }

    //清理垃圾
    private fun clearCache(){
        deleteDir(getContext().cacheDir)
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) deleteDir(getContext().externalCacheDir)
        val compressFile = File(OssInformation.downloadCompressDirectory)
        if(compressFile.exists()){
            val compressFileList = compressFile.listFiles()
            for(file in compressFileList !!) deleteDir(file)
        }
        val originalFile = File(OssInformation.downloadOriginalDirectory)
        if(originalFile.exists()){
            val originalFileList = originalFile.listFiles()
            for(file in originalFileList !!) deleteDir(file)
        }
    }
    //清理垃圾中的清理文件夹
    private fun deleteDir(dir: File?) {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()!!
            for (child in children) {
                deleteDir(File(dir, child))
            }
        }
        dir?.delete()
    }
}