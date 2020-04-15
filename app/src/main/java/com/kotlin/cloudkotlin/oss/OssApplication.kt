package com.kotlin.cloudkotlin.oss

import android.app.Application
import android.content.Context
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.OSS
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSCustomSignerCredentialProvider
import com.alibaba.sdk.android.oss.common.utils.OSSUtils

class OssApplication : Application(){


    companion object{
        //获取全局Context
        lateinit var Context:Context
        //oss实例
        lateinit var oss:OSS
    }

    //创建oss所需
    private lateinit var ossProvider:OSSCredentialProvider
    private  val accessKey = OssInformation.ossAccessKeyId
    private  val accessSecret = OssInformation.ossAccessKeySecret
    private val endPoint = OssInformation.ossEndpoint



    override fun onCreate() {
        super.onCreate()
        Context = applicationContext

        ossProvider = object : OSSCustomSignerCredentialProvider(){
            override fun signContent(content: String?): String {
                return OSSUtils.sign(accessKey, accessSecret, content)
            }
        }

        //ossClient的基础配置
        val conf = ClientConfiguration().apply {
            connectionTimeout = 10 * 1000   // 连接超时
            socketTimeout = 10 * 1000   // socket超时
            maxConcurrentRequest = 6    // 最大并发请求书
            maxErrorRetry = 2   // 失败后最大重试次数
        }
        oss = OSSClient(Context, endPoint, ossProvider, conf)
    }

}