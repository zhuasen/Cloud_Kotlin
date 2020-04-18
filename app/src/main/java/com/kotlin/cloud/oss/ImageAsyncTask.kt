package com.kotlin.cloud.oss

import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import androidx.core.content.contentValuesOf
import cn.pedant.SweetAlert.SweetAlertDialog
import cn.pedant.SweetAlert.SweetAlertDialog.CUSTOM_IMAGE_TYPE
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.model.GetObjectRequest
import com.alibaba.sdk.android.oss.model.ObjectMetadata
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.kotlin.cloud.R
import com.kotlin.cloud.activity.LargerImage
import com.kotlin.cloud.activity.LargerImage.Companion.clickForLargerImageName
import com.kotlin.cloud.activity.MainActivity
import com.kotlin.cloud.activity.MainActivity.Companion.imgPathForUploadList
import com.kotlin.cloud.activity.MainActivity.Companion.uploadProgressDialog
import com.kotlin.cloud.activity.SelectPicture
import com.kotlin.cloud.activity.SelectPicture.Companion.canRefresh
import com.kotlin.cloud.activity.SelectPicture.Companion.downloadEnd
import com.kotlin.cloud.activity.SelectPicture.Companion.downloadOriginalImageDialog
import com.kotlin.cloud.activity.SelectPicture.Companion.downloadStart
import com.kotlin.cloud.activity.SelectPicture.Companion.imageForSelectAdapter
import com.kotlin.cloud.activity.SelectPicture.Companion.imagePathForAdapterList
import com.kotlin.cloud.activity.SelectPicture.Companion.imageRecycleView
import com.kotlin.cloud.activity.SelectPicture.Companion.imageRefreshLayout
import com.kotlin.cloud.activity.SelectPicture.Companion.selectImageNameList
import com.kotlin.cloud.kotlinUtil.*
import com.kotlin.cloud.usefulClass.imageUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

/**
 * 负责图片的异步任务，功能：
 * @author ailanxier
 * @Date 2020-04-18
 * @param taskType 确定异步任务的类型
 * @see onPreExecute 执行任务前初始化信息
 * @see doInBackground 网络请求主体，并实时传递进度给onProgressUpdate，把执行情况传递给onPostExecute
 * @see onProgressUpdate 接受进度传递，UI显示
 * @see onPostExecute 根据结果返回执行相应逻辑
 *
 * @weakness 当发生任何异常时，如果继续尝试加载会导致灾难性bug
 * @weakness 将太多逻辑绑定在几个var类型的变量上
 *
 * **/
class ImageAsyncTask (private val taskType: Int) : AsyncTask<Unit,Int, Int>(){

    override fun onPreExecute() {
        lastProgress = 0
        curProgress = 0
        account = getInfoString("account")
    }

    override fun doInBackground(vararg params: Unit?): Int {
        when(taskType){
            //上传图片
            UPLOAD_FILE_TASK->{
                try {
                    //获取object数，先判断是不是空(用是否有“1”这个文件来判断)
                    if (!oss.doesObjectExist(account + randomCode, "1")) {
                        objectNumberInTotal = 0
                    } else {
                        var maxNumber = maxObjectNumber
                        var targetNumber: Int
                        var minNumber = 1
                        while (minNumber <= maxNumber) {
                            targetNumber = (minNumber + maxNumber) / 2
                            if (oss.doesObjectExist(account + randomCode, targetNumber.toString())) {
                                minNumber = targetNumber + 1
                                //一个存在但是他的后一个不存在，就是已经找到最后一个了
                                if (!oss.doesObjectExist(account + randomCode, (targetNumber + 1).toString())) {
                                    objectNumberInTotal = targetNumber
                                    break
                                }
                            } else maxNumber = targetNumber - 1
                        }
                    }
                    //当上次已经加载完时，本次上传后就可以刷新了
                    if(objectNumberInTotal == imagePathForAdapterList.size)  canRefresh = true
                    //正式上传,这里的filePath是绝对路径，将文件命名为（++后的objectNumber）
                    for (filePath in imgPathForUploadList) {
                        val uploadFileTask = PutObjectRequest(account + randomCode, "${++objectNumberInTotal}", filePath)
                        //设置object的类型
                        val metadata = ObjectMetadata()
                        metadata.contentType = imageUtil.getContentType(filePath.substring(filePath.lastIndexOf(".")))
                        uploadFileTask.metadata = metadata
                        //返回上传进度
                        oss.putObject(uploadFileTask)
                        publishProgress(++curProgress)
                    }
                    return UPLOAD_SUCCESS //for循环结束就上传成功了
                }catch ( e: ClientException) {
                    return UPLOAD_NETWORK_FAIL;
                }catch (e: ServiceException){
                    return UPLOAD_SERVICE_FAIL;
                }
            }
            //第一次下载图片
            DOWNLOAD_FIRST_COMPRESS_TASK->{
                try {
                    //获取object数，先判断是不是空
                    if (!oss.doesObjectExist(account + randomCode, "1")) {
                        objectNumberInTotal = 0
                        return DOWNLOAD_FIRST_NULL_FILE
                    } else {
                        var maxNumber = maxObjectNumber
                        var targetNumber: Int
                        var minNumber = 1
                        while (minNumber <= maxNumber) {
                            targetNumber = (minNumber + maxNumber) / 2
                            if (oss.doesObjectExist(account + randomCode, targetNumber.toString())) {
                                minNumber = targetNumber + 1
                                //一个存在但是他的后一个不存在，就是已经找到最后一个了
                                if (!oss.doesObjectExist(account + randomCode, (targetNumber + 1).toString())) {
                                    objectNumberInTotal = targetNumber
                                    break
                                }
                            } else maxNumber = targetNumber - 1
                        }
                    }
                    val downloadEnd = min(firstDownloadNumber, objectNumberInTotal)
                    SelectPicture.downloadEnd = downloadEnd
                    //初次下载编号为1到downloadEnd的所有图片
                    for (i in 1..downloadEnd){
                        //构造下载文件请求
                        val downloadFirstRequest = GetObjectRequest(account + randomCode, i.toString())
                        //oss自带图片处理，w是宽300dp，h是长350dp，m_fill是压缩居中裁剪
                        downloadFirstRequest.setxOssProcess("image/resize,m_fill,h_350,w_300")
                        // 同步执行下载请求，返回结果
                        val getResult =oss.getObject(downloadFirstRequest)
                        //写入本地的缓存图片文件名与在bucket中存储的名字一样
                        val firstInputStream = getResult.objectContent
                        val firstInputBuffer = ByteArray(2048)
                        val filePath = "$compressPictureFile/$i.jpg"
                        FileOutputStream(filePath).use {
                            var alreadyInputByte = firstInputStream.read(firstInputBuffer)
                            while (alreadyInputByte!=-1){
                                it.write(firstInputBuffer,0,alreadyInputByte)
                                alreadyInputByte = firstInputStream.read(firstInputBuffer)
                            }
                            firstInputStream.close()
                        }
                        publishProgress(i)
                    }
                    return DOWNLOAD_FIRST_COMPRESS_SUCCESS
                }catch ( e: ClientException) {
                    return DOWNLOAD_FIRST_NETWORK_FAIL
                }catch (e: ServiceException){
                    return DOWNLOAD_FIRST_SERVICE_FAIL
                }catch (e :IOException){
                    return DOWNLOAD_FIRST_IOE_FAIL
                }
            }
            //点击图片放大（下载一张原图）
            DOWNLOAD_ONE_ORIGINAL_TASK->{
                try {
                    val downloadOneOriginalRequest = GetObjectRequest(account + randomCode, clickForLargerImageName)
                    val getResult =oss.getObject(downloadOneOriginalRequest)
                    //写入本地的缓存图片文件名与在bucket中存储的名字一样
                    val downloadOneInputStream = getResult.objectContent
                    val downloadOneInputBuffer = ByteArray(2048)
                    val filePath = "$originalPictureFile/$clickForLargerImageName.jpg"
                    FileOutputStream(filePath).use {
                        var alreadyInputByte = downloadOneInputStream.read(downloadOneInputBuffer)
                        while (alreadyInputByte!=-1){
                            it.write(downloadOneInputBuffer,0,alreadyInputByte)
                            alreadyInputByte = downloadOneInputStream.read(downloadOneInputBuffer)
                        }
                        downloadOneInputStream.close()
                    }
                        return DOWNLOAD_ONE_PICTURE_SUCCESS
                    }catch ( e: ClientException) {
                        return DOWNLOAD_ONE_NETWORK_FAIL
                    }catch (e: ServiceException){
                         return DOWNLOAD_ONE_SERVICE_FAIL
                    }catch (e :IOException){
                         return DOWNLOAD_ONE_IOE_FAIL
                    }
            }
            //上拉加载更多图片
            DOWNLOAD_MORE_COMPRESS_TASK->{
                //下载编号为downloadStart到downloadEnd的所有图片
                try {
                    for (i in downloadStart..downloadEnd) {
                        //构造下载文件请求
                        val downloadMoreFileRequest = GetObjectRequest(account + randomCode, "$i")
                        downloadMoreFileRequest.setxOssProcess("image/resize,m_fill,h_350,w_300")
                        // 同步执行下载请求，返回结果
                        val getResult = oss.getObject(downloadMoreFileRequest)
                        //写入本地的缓存图片文件名与在bucket中存储的名字一样
                        val moreInputStream = getResult.objectContent
                        val moreInputBuffer = ByteArray(2048)
                        val moreFilePath = "$compressPictureFile/$i.jpg"
                        FileOutputStream(moreFilePath).use {
                            var alreadyInputByte = moreInputStream.read(moreInputBuffer)
                            while (alreadyInputByte!=-1){
                                it.write(moreInputBuffer,0,alreadyInputByte)
                                alreadyInputByte = moreInputStream.read(moreInputBuffer)
                            }
                            moreInputStream.close()
                        }
                        publishProgress(curProgress++)
                    }
                } catch (e: ClientException) {
                    return DOWNLOAD_MORE_NETWORK_FAIL
                } catch (e: ServiceException) {
                    return DOWNLOAD_MORE_SERVICE_FAIL
                } catch (e: IOException) {
                    return DOWNLOAD_MORE_IOE_FAIL
                }
                return DOWNLOAD_MORE_COMPRESS_SUCCESS
            }
            //下拉查看刷新图片
            DOWNLOAD_REFRESH_TASK->{
                //下载编号为downloadStart到downloadEnd的所有图片
                try {
                    for (i in downloadStart..downloadEnd) {
                        //构造下载文件请求
                        val downloadRefreshFileRequest = GetObjectRequest(account + randomCode, i.toString())
                        downloadRefreshFileRequest.setxOssProcess("image/resize,m_fill,h_350,w_300")
                        // 同步执行下载请求，返回结果
                        val getResult = oss.getObject(downloadRefreshFileRequest)
                        //写入本地的缓存图片文件名与在bucket中存储的名字一样
                        val refreshInputStream = getResult.objectContent
                        val refreshInputBuffer = ByteArray(2048)
                        val refreshFilePath = "$compressPictureFile/$i.jpg"
                        FileOutputStream(refreshFilePath).use {
                            var alreadyInputByte = refreshInputStream.read(refreshInputBuffer)
                            while (alreadyInputByte!=-1){
                                it.write(refreshInputBuffer,0,alreadyInputByte)
                                alreadyInputByte = refreshInputStream.read(refreshInputBuffer)
                            }
                            refreshInputStream.close()
                        }
                    }
                } catch (e: ClientException) {
                    return DOWNLOAD_REFRESH_NETWORK_FAIL
                } catch (e: ServiceException) {
                    return DOWNLOAD_REFRESH_SERVICE_FAIL
                } catch (e: IOException) {
                    return DOWNLOAD_REFRESH_IOE_FAIL
                }
                return DOWNLOAD_REFRESH_SUCCESS
            }
            //下载已选择的原图
            DOWNLOAD_ORIGINAL_TASK->{
                try {
                    for (fileName in selectImageNameList){
                        val values = contentValuesOf(MediaStore.MediaColumns.DISPLAY_NAME to "$fileName.jpeg",
                            MediaStore.MediaColumns.MIME_TYPE to "image/jpg" )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                        } else {
                            values.put(MediaStore.MediaColumns.DATA, "${Environment.getExternalStorageDirectory().path}/${Environment.DIRECTORY_DCIM}/$fileName")
                        }
                        val uri = getContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        val downloadFilePath = File("${OssInformation.downloadOriginalDirectory}/${fileName}.jpg")
                        //根据该图片原图是否存在选择不同的输入流
                        val originalInputStream  = if(downloadFilePath.exists()) FileInputStream(downloadFilePath.absolutePath)
                        else{
                            val downloadOriginalRequest = GetObjectRequest(account + randomCode, fileName)
                            val getResult  = oss.getObject(downloadOriginalRequest)
                            getResult.objectContent
                        }
                        if (uri != null) {
                            val outputStream = getContext().contentResolver.openOutputStream(uri)
                            if (outputStream != null) {
                                val originalInputBuffer = ByteArray(2048)
                                outputStream.use {
                                    var alreadyInputByte = originalInputStream.read(originalInputBuffer)
                                    while (alreadyInputByte!=-1){
                                        it.write(originalInputBuffer,0,alreadyInputByte)
                                        alreadyInputByte = originalInputStream.read(originalInputBuffer)
                                    }
                                    outputStream.close()
                                }
                            }
                        }
                        publishProgress(++curProgress)
                    }
                }catch (e:ClientException ) {
                    return DOWNLOAD_ORIGINAL_NETWORK_FAIL;
                } catch (e:ServiceException ) {
                    return DOWNLOAD_ORIGINAL_SERVICE_FAIL;
                }catch (e:IOException ){
                    return DOWNLOAD_ORIGINAL_IOE_FAIL;
                }
                return DOWNLOAD_ORIGINAL_SUCCESS
            }
            else->return RETURN_ERROR_TYPE
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {
        when(taskType){
            //上传图片时进度回调
            UPLOAD_FILE_TASK->{
                if (values[0]!! > lastProgress) {
                    lastProgress = values[0]!!
                    uploadProgressDialog?.contentText = "上传进度: $lastProgress / ${imgPathForUploadList.size}"
                    when ((((lastProgress.toFloat())/ imgPathForUploadList.size)*100).toInt()) {
                        in 1..24 -> uploadProgressDialog?.progressHelper?.barColor =  getColor(R.color.green)
                        in 25..49 -> uploadProgressDialog?.progressHelper?.barColor = getColor(R.color.mildPurple)
                        in 50..74 -> uploadProgressDialog?.progressHelper?.barColor = getColor(R.color.orange)
                        in 74..100 -> uploadProgressDialog?.progressHelper?.barColor = getColor(R.color.red)
                    }
                }
            }
            //初次下载图片
            DOWNLOAD_FIRST_COMPRESS_TASK->{
                values[0]?.let{
                    imagePathForAdapterList.add("${compressPictureFile.absolutePath}/$it.jpg")
                    imageForSelectAdapter.notifyItemInserted(it)
                    when (it) {
                        9-> SelectPicture.downloadFirstCompressImageDialog?.progressHelper?.barColor = getColor(R.color.orange)
                        15 -> SelectPicture.downloadFirstCompressImageDialog?.progressHelper?.barColor = getColor(R.color.red)
                    }
                }
            }
            //下载原图
            DOWNLOAD_ORIGINAL_TASK->{
                values[0]?.let {
                    if(it > lastProgress){
                        lastProgress = it
                        downloadOriginalImageDialog?.contentText = "下载进度：$lastProgress / ${selectImageNameList.size}"
                        when ((((lastProgress.toFloat())/ selectImageNameList.size)*100).toInt()) {
                            in 1..24 -> downloadOriginalImageDialog?.progressHelper?.barColor =  getColor(R.color.green)
                            in 25..49 -> downloadOriginalImageDialog?.progressHelper?.barColor = getColor(R.color.mildPurple)
                            in 50..74 -> downloadOriginalImageDialog?.progressHelper?.barColor = getColor(R.color.orange)
                            in 74..100 -> downloadOriginalImageDialog?.progressHelper?.barColor = getColor(R.color.red)
                        }
                    }
                }
            }
            //加载更多图片
            DOWNLOAD_MORE_COMPRESS_TASK->{
                values[0]?.let {
                    if((it == 0 && downloadEnd - downloadStart < 3)||it == 3){
                        imageRefreshLayout?.finishLoadMore()
                        //延迟一会，不然会显示异常
                        Handler().postDelayed({imageRefreshLayout?.setEnableLoadMore(false)},1400)
                    }
                    imagePathForAdapterList.add("${compressPictureFile.absolutePath}/${downloadStart + it}.jpg")
                    imageForSelectAdapter.notifyItemInserted(downloadStart+ it)
                }
            }
        }
    }

    override fun onPostExecute(result: Int?) {
        when(result){
            //上传图片成功
            UPLOAD_SUCCESS->{
                uploadProgressDialog?.apply {
                    titleText = "上传成功"
                    contentText = "您已上传"+ curProgress +"张图片"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                }
                imgPathForUploadList.clear()
                imageRefreshLayout?.setNoMoreData(false)
            }
            //上传图片失败<-网络异常
            UPLOAD_NETWORK_FAIL->{
                uploadProgressDialog ?.apply {
                    titleText = "图片上传失败"
                    contentText = "请检查网络连接"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                }
            }
            //上传图片失败<-服务器异常
            UPLOAD_SERVICE_FAIL->{
                uploadProgressDialog?.apply {
                    titleText = "图片上传失败"
                    contentText = "服务器异常"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                }
            }

            //初始化压缩图片成功
            DOWNLOAD_FIRST_COMPRESS_SUCCESS->{
                SelectPicture.downloadFirstCompressImageDialog?.dismissWithAnimation()
            }
            //初始化压缩图片失败<-网络原因
            DOWNLOAD_FIRST_NETWORK_FAIL->{
                SelectPicture.downloadFirstCompressImageDialog?.apply {
                    titleText = "网络异常，下载失败"
                    contentText = "请稍后重试"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                }
            }
            //初始化压缩图片失败<-服务器原因
            DOWNLOAD_FIRST_SERVICE_FAIL-> {
                SelectPicture.downloadFirstCompressImageDialog?.apply {
                    titleText = "服务器异常，下载失败"
                    contentText = "请稍后重试"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                }
            }
            //初始化压缩图片失败<-io流原因
            DOWNLOAD_FIRST_IOE_FAIL-> {
                SelectPicture.downloadFirstCompressImageDialog?.apply {
                    titleText = "查看图片失败"
                    contentText = "请稍后重试"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                }
            }
            //初始化时发现该用户并无上传任何图片
            DOWNLOAD_FIRST_NULL_FILE->{
                SelectPicture.downloadFirstCompressImageDialog?.apply {
                    titleText = "您还未上传任何图片"
                    contentText = "先去选择图片上传吧"
                    confirmText = "OK"
                    setCustomImage(getDrawable(R.drawable.found_zero))
                    setConfirmClickListener {
                        it.dismissWithAnimation()
                        startAction<MainActivity>()
                    }
                    changeAlertType(CUSTOM_IMAGE_TYPE)
                }
            }

             //仅下载一张原图质量的图片（用来放大）成功
            DOWNLOAD_ONE_PICTURE_SUCCESS->{
                //打开大图，将旋转progressBar消掉
                LargerImage.LargerImageProgressBar?.apply{
                    stop()
                    visibility = View.GONE
                }
                LargerImage.showImage()
            }
            //仅下载一张原图质量的图片（用来放大）失败<-网络原因
            DOWNLOAD_ONE_NETWORK_FAIL->{
                LargerImage.LargerImageProgressBar?.apply{
                    stop()
                    visibility = View.GONE
                }
                "加载大图失败，请检查网络连接".toastShort()
            }
            //仅下载一张原图质量的图片（用来放大）失败<-网络原因
            DOWNLOAD_ONE_SERVICE_FAIL->{
                LargerImage.LargerImageProgressBar?.apply{
                    stop()
                    visibility = View.GONE
                }
                "加载大图失败，服务器异常".toastShort()
            }
            //仅下载一张原图质量的图片（用来放大）失败<-网络原因
            DOWNLOAD_ONE_IOE_FAIL->{
                LargerImage.LargerImageProgressBar?.apply{
                    stop()
                    visibility = View.GONE
                }
                "加载大图失败，输入输出流异常".toastShort()
            }

            //上拉刷新，下载更多压缩图片成功
            DOWNLOAD_MORE_COMPRESS_SUCCESS->{
                imageRefreshLayout?.setEnableLoadMore(true)
            }
            //上拉刷新，下载更多压缩图片失败<-网络原因
            DOWNLOAD_MORE_NETWORK_FAIL->{
                "网络异常".toastShort()
                imageRefreshLayout?.finishLoadMore(false)
            }
            //上拉刷新，下载更多压缩图片失败<-服务器原因
            DOWNLOAD_MORE_SERVICE_FAIL->{
                "服务器异常".toastShort()
                imageRefreshLayout?.finishLoadMore(false)
            }
            //上拉刷新，下载更多压缩图片失败<-io流原因
            DOWNLOAD_MORE_IOE_FAIL->{
                "输入输出流异常".toastShort()
                imageRefreshLayout?.finishLoadMore(false)
            }

            //下拉刷新图片成功
            DOWNLOAD_REFRESH_SUCCESS->{
                val compressFiles = compressPictureFile.listFiles()
                //将下载的新压缩图片放到adapter的list中，然后更新
                for (file in compressFiles!!) {
                    if (file.name.getIntByIndex('.') in downloadStart..downloadEnd)
                        imagePathForAdapterList.add(file.absolutePath)
                }
                imageForSelectAdapter.notifyItemInserted(downloadStart)
                //结束加载更多
                imageRefreshLayout?.finishRefresh()
                Handler().postDelayed({ imageRecycleView?.smoothScrollToPosition(imagePathForAdapterList.size)},430)
            }
            //下载刷新图片失败<-网络异常
            DOWNLOAD_REFRESH_NETWORK_FAIL->{
                "网络连接失败".toastShort()
                imageRefreshLayout?.finishRefresh()
            }
            //下载刷新图片失败<-服务器异常
            DOWNLOAD_REFRESH_SERVICE_FAIL->{
                "服务器异常".toastShort()
                imageRefreshLayout?.finishRefresh()
            }
            //下载刷新图片失败<-io流异常
            DOWNLOAD_REFRESH_IOE_FAIL->{
                "输入输出流异常".toastShort()
                imageRefreshLayout?.finishRefresh()
            }

            //下载原图成功
            DOWNLOAD_ORIGINAL_SUCCESS->{
                downloadOriginalImageDialog?.apply {
                    titleText = "下载成功"
                    contentText = "${selectImageNameList.size} 张图片已下载回到您的相册中"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                }
                selectImageNameList.clear()
            }
            //下载原图回相册失败<-网络原因
            DOWNLOAD_ORIGINAL_NETWORK_FAIL->{
                downloadOriginalImageDialog?.apply {
                    titleText = "图片下载失败"
                    contentText = "请检查网络连接"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                }
                selectImageNameList.clear()
            }
            //下载原图回相册失败<-服务器原因
            DOWNLOAD_ORIGINAL_SERVICE_FAIL->{
                downloadOriginalImageDialog?.apply {
                    titleText = "图片下载失败"
                    contentText = "服务器连接失败"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                }
                selectImageNameList.clear()
            }
            //下载原图回相册失败<-io流原因
            DOWNLOAD_ORIGINAL_IOE_FAIL->{
                downloadOriginalImageDialog?.apply {
                    titleText = "图片下载失败"
                    contentText = "输入输出流异常"
                    confirmText = "OK"
                    setConfirmClickListener { it.dismissWithAnimation() }
                    changeAlertType(SweetAlertDialog.WARNING_TYPE)
                }
                selectImageNameList.clear()
            }
        }
    }

    companion object{
        private const val randomCode = OssInformation.ossRandomCode
        private val oss = OssApplication.oss
        private var lastProgress = 0
        private var curProgress = 0
        private lateinit var account : String
        var objectNumberInTotal = 0
        private const val maxObjectNumber = 2000
        private const val firstDownloadNumber = 18

        //指定缓存的文件夹
        private val compressPictureFile: File = File(OssInformation.downloadCompressDirectory)
        private val originalPictureFile: File = File(OssInformation.downloadOriginalDirectory)

        //首次下载压缩图片指令
        const val DOWNLOAD_FIRST_COMPRESS_TASK = 1
        private const val DOWNLOAD_FIRST_COMPRESS_SUCCESS = 2
        private const val DOWNLOAD_FIRST_NETWORK_FAIL = 3
        private const val DOWNLOAD_FIRST_SERVICE_FAIL = 4
        private const val DOWNLOAD_FIRST_IOE_FAIL = 5
        private const val DOWNLOAD_FIRST_NULL_FILE = 6


        //再次下载压缩图片指令
        const val DOWNLOAD_MORE_COMPRESS_TASK = 7
        private const val DOWNLOAD_MORE_COMPRESS_SUCCESS = 8
        private const val DOWNLOAD_MORE_NETWORK_FAIL = 9
        private const val DOWNLOAD_MORE_SERVICE_FAIL = 10
        private const val DOWNLOAD_MORE_IOE_FAIL = 11

        //下载原图指令
        const val DOWNLOAD_ORIGINAL_TASK = 12
        private const val DOWNLOAD_ORIGINAL_SUCCESS = 13
        private const val DOWNLOAD_ORIGINAL_NETWORK_FAIL = 14
        private const val DOWNLOAD_ORIGINAL_SERVICE_FAIL = 15
        private const val DOWNLOAD_ORIGINAL_IOE_FAIL = 16

        //上传原图指令
        const val UPLOAD_FILE_TASK = 17
        private const val UPLOAD_SUCCESS = 18
        private const val UPLOAD_NETWORK_FAIL = 19
        private const val UPLOAD_SERVICE_FAIL = 20

        //下载一张图片供图片放大
        const val DOWNLOAD_ONE_ORIGINAL_TASK = 21
        private const val DOWNLOAD_ONE_PICTURE_SUCCESS = 22
        private const val DOWNLOAD_ONE_NETWORK_FAIL = 23
        private const val DOWNLOAD_ONE_SERVICE_FAIL = 24
        private const val DOWNLOAD_ONE_IOE_FAIL = 25

        //下拉刷新
        const val DOWNLOAD_REFRESH_TASK = 26
        private const val DOWNLOAD_REFRESH_SUCCESS = 27
        private const val DOWNLOAD_REFRESH_IOE_FAIL = 28
        private const val DOWNLOAD_REFRESH_NETWORK_FAIL = 29
        private const val DOWNLOAD_REFRESH_SERVICE_FAIL = 306

        //不符合要求的taskType
        private const val RETURN_ERROR_TYPE = 100
    }
}


