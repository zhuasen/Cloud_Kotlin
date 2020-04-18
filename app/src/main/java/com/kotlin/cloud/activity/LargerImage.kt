package com.kotlin.cloud.activity

import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.kotlin.cloud.R
import com.kotlin.cloud.kotlinUtil.getContext
import com.kotlin.cloud.kotlinUtil.getStrByIndex
import com.kotlin.cloud.oss.ImageAsyncTask
import com.kotlin.cloud.oss.OssApplication
import com.kotlin.cloud.oss.OssInformation
import com.tt.whorlviewlibrary.WhorlView
import kotlinx.android.synthetic.main.larger_image.*
import me.panpf.sketch.SketchImageView
import java.io.File

/**
 * 在选择图片时点击图片，可以放大图片的activity
 *
 * @author ailanxier
 * @date  2020-4-18
 *
 * 1、先新建一个文件夹放置要打开图片的原图
 * 2、通过接收用户点击图片的uri解析得到图片的序号
 * 3、如果已经存在与文件夹中（之前点开过），就直接打开，否则调用异步任务下载原图
 * 4、当用户下载这张图片时，可以直接从本地复制过去
 * 5、 图片随手指伸缩放大（应用一个库替代ImageView）
 * bug:
 * 1、第一次打开和第二次打开清晰度不一样（相片图片会一样，但是其他照片就会不一样：壁纸，截图之类的）
 *
 *
 * */
class LargerImage : BaseActivity() {

    companion object{
        var LargerImageProgressBar:WhorlView?=null
        private val originalImageDirectory = File(OssInformation.downloadOriginalDirectory)
        private lateinit var clickForLargerImageFile : File
        lateinit var clickForLargerImageUri : String
        lateinit var clickForLargerImageName : String
        lateinit var BigImageView: SketchImageView
        //Glide显示图片
        fun showImage()=Glide.with(getContext()).load(clickForLargerImageFile.absolutePath).into(BigImageView)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.larger_image)

        //没有存放原图的文件夹就新建一个
        if (!originalImageDirectory.exists()) originalImageDirectory.mkdirs()
        BigImageView = findViewById(R.id.largerImageView)
        //图片设置可以缩放（双指放大）
        largerImageView.isZoomEnabled = true
        LargerImageProgressBar = findViewById(R.id.largerImageProgressBar)
        //用户点击的缩略图的uri(接收参数）
        clickForLargerImageUri = intent.getStringExtra("imageUri")!!
        //缩略图名字(就是序号）
        clickForLargerImageName = clickForLargerImageUri.getStrByIndex('/',1).getStrByIndex('.')
        //看看之前有没有打开过这个图片查看，如果有就不重新下载了，直接加载
        clickForLargerImageFile = File("${originalImageDirectory.absolutePath}/$clickForLargerImageName.jpg")
        if (clickForLargerImageFile.exists()) {showImage() }
        else {
            largerImageProgressBar.apply {
                visibility = View.VISIBLE
                start() }
            //异步下载图片到指定的原图文件夹
            ImageAsyncTask(ImageAsyncTask.DOWNLOAD_ONE_ORIGINAL_TASK).execute()
        }

        largerImageView.setOnClickListener { ActivityCompat.finishAfterTransition(this@LargerImage) }
    }

    override fun onResume() {
        super.onResume()
        this.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    override fun onRestart() {
        super.onRestart()
        largerImageProgressBar.visibility = View.GONE
    }
}
