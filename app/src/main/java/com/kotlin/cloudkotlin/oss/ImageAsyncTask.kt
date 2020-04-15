package com.kotlin.cloudkotlin.oss

import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import com.kotlin.cloudkotlin.kotlinUtil.getMySharePref
import kotlin.reflect.KProperty

class ImageAsyncTask (private val taskType: Int) : AsyncTask<Unit, Float?, Int>(){
    override fun doInBackground(vararg params: Unit?): Int {
        TODO("Not yet implemented")
    }

    companion object{
        private const val randomCode = OssInformation.ossRandomCode
        private val oss = OssApplication.oss
        private var lastProgress = 0
        private var curProgress = 0
        private val account by lazy {
            getMySharePref().getString("account","")
        }
        private val showImageInPageNumber by lazy {
            21
        }
        //首次下载压缩图片指令
        const val DOWNLOAD_FIRST_COMPRESS_FILE = 1
        private const val DOWNLOAD_FIRST_COMPRESS_SUCCESS = 2
        private const val DOWNLOAD_FIRST_NETWORK_FAIL = 3
        private const val DOWNLOAD_FIRST_SERVICE_FAIL = 4
        private const val DOWNLOAD_FIRST_IOE_FAIL = 5
        private const val DOWNLOAD_FIRST_NULL_FILE = 6

        //直接从文件夹添加图片
        const val READ_COMPRESS_FILE_TASK = 7
        private const val READ_COMPRESS_FILE_SUCCESS = 8

        //再次下载压缩图片指令
        const val DOWNLOAD_MORE_COMPRESS_FILE = 9
        private const val DOWNLOAD_MORE_COMPRESS_SUCCESS = 10
        private const val DOWNLOAD_MORE_NETWORK_FAIL = 11
        private const val DOWNLOAD_MORE_SERVICE_FAIL = 12
        private const val DOWNLOAD_MORE_IOE_FAIL = 13

        //下载原图指令
        const val DOWNLOAD_ORIGINAL_FILE = 14
        private const val DOWNLOAD_ORIGINAL_SUCCESS = 15
        private const val DOWNLOAD_ORIGINAL_NETWORK_FAIL = 16
        private const val DOWNLOAD_ORIGINAL_SERVICE_FAIL = 17
        private const val DOWNLOAD_ORIGINAL_IOE_FAIL = 18

        //上传原图指令
        const val UPLOAD_FILE_TASK = 19
        private const val UPLOAD_SUCCESS = 20
        private const val UPLOAD_NETWORK_FAIL = 21
        private const val UPLOAD_SERVICE_FAIL = 22

        //下载一张图片供图片放大
        const val DOWNLOAD_ONE_PICTURE = 27
        private const val DOWNLOAD_ONE_PICTURE_SUCCESS = 28
        private const val DOWNLOAD_ONE_NETWORK_FAIL = 29
        private const val DOWNLOAD_ONE_SERVICE_FAIL = 30
        private const val DOWNLOAD_ONE_IOE_FAIL = 31

        //上传新图片后且没有满一页要求刷新的情况
        const val DOWNLOAD_REFRESH_PICTURE = 32
        private const val DOWNLOAD_REFRESH_SUCCESS = 33
        private const val DOWNLOAD_REFRESH_IOE_FAIL = 34
        private const val DOWNLOAD_REFRESH_NETWORK_FAIL = 35
        private const val DOWNLOAD_REFRESH_SERVICE_FAIL = 36

        //不符合要求的taskType
        private const val RETURN_ERROR_TYPE = 100
    }
}


