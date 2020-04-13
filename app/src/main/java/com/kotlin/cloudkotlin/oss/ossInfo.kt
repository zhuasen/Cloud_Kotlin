package com.kotlin.cloudkotlin.oss

import android.os.Environment

object ossInfo {

    //阿里云oss常量
    // 访问的endpoint地址
     const val ossEndpoint = "http://oss-cn-shenzhen.aliyuncs.com"
    //aliyun主账号的两个重要密码
     const val ossAccessKeyId = "LTAI4Fwn5UsoSy6KQCZ1qQhr"
    const val ossAccessKeySecret = "UjvEFb9rxPfW6it36Yfq826fBId2q4"

    //压缩图片缓存地址(storage/emulated/0/手机的文件管理默认就是这个目录下的东西)
    // Download/cloud_compress_picture
    val downloadCompressDirectory = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
            + "/" + "cloud_compress_picture")

    //原质量图片缓存地址(storage/emulated/0/手机的文件管理默认就是这个目录下的东西)
    // Download/cloud_compress_picture
    val downloadOriginalDirectory = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
            + "/" + "cloud_original_picture")

    //aliyun为防止bucket重名简单的添加后缀
    const val ossRandomCode = "-intelligentimageshrinktoolformobileterminal"
}