package com.kotlin.cloud.activity

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.google.android.material.bottomappbar.BottomAppBar
import com.kotlin.cloud.R
import com.kotlin.cloud.kotlinUtil.getStrByIndex
import com.kotlin.cloud.kotlinUtil.toastShort
import com.kotlin.cloud.oss.ImageAsyncTask
import com.kotlin.cloud.oss.ImageAsyncTask.Companion.objectNumberInTotal
import com.kotlin.cloud.oss.OssInformation
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import kotlinx.android.synthetic.main.select_layout.*
import java.io.File
import java.util.*
import kotlin.math.min

class SelectPicture : BaseActivity() {

    companion object{
        //判断是否可以刷新的依据
        var canRefresh = false
        //在异步任务中提示download文件名的范围（靠数字识别文件）
        var downloadStart = 0
        var downloadEnd = 0
        //记录已经选择的图片数
        private var alreadySelectNumber = 0
        //一次加载多少张图片，可以作为用户设置选项
        private const val moreDownloadNumber = 42

        //刷新布局的实例
        var imageRefreshLayout: SmartRefreshLayout? = null
        //recyclerview的实例
        var imageRecycleView : RecyclerView ?= null
        //存放图片在手机中真实路径，适配器的list，核心数据
        var imagePathForAdapterList= ArrayList<String>()
        //recyclerview的适配器
        lateinit var imageForSelectAdapter : ImageAdapter

        //CheckBox的选择情况
        private var checkStatus = HashMap<Int, Boolean>()
        //存放用户已选择要下载的图片uri的list
        private var selectImageUriList = ArrayList<String>()
        //装已选择要下载的图片名称的set容器，通过selectImageUriList解析得到,用于给异步任务下载对象
        var selectImageNameList = ArrayList<String>()

        //指定缓存的文件夹
        private var downloadCompressFile = File(OssInformation.downloadCompressDirectory)
        //初始化压缩图片的dialog
        var downloadFirstCompressImageDialog: SweetAlertDialog? = null
        //下载原图回来的dialog
        var downloadOriginalImageDialog: SweetAlertDialog? = null

        //下拉隐藏的工具栏
        lateinit var SelectBottomAppBar : BottomAppBar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_layout)

        SelectBottomAppBar = findViewById(R.id.selectBottomAppBar)
        imageRecycleView = findViewById(R.id.imageForChoose_RecyclerView)
        showSelectText()
        //给recyclerview添加manager和adapter
        imageForChoose_RecyclerView.layoutManager = GridLayoutManager(this,3)
        imageForSelectAdapter = ImageAdapter()
        imageForChoose_RecyclerView.adapter = imageForSelectAdapter

        val compressFile = downloadCompressFile.listFiles()
        //首次加载
        if(compressFile!!.isEmpty()){
            downloadFirstCompressImageDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                titleText = "正在返回图片，请稍候"
                setCancelable(false)
                progressHelper.barColor = getColor(R.color.mildPurple)
                show()
            }
            downloadStart = 1
            //downloadEnd要通过获取object总数才能确定，放在异步任务中设置
            ImageAsyncTask(ImageAsyncTask.DOWNLOAD_FIRST_COMPRESS_TASK).execute()
        }

        imageRefreshLayout = findViewById(R.id.imageForChoose_RefreshLayout)
        //上拉加载更多图片
        imageForChoose_RefreshLayout.setOnLoadMoreListener{ downloadMoreImage() }
        //下拉滑动到最新图片
        imageForChoose_RefreshLayout.setOnRefreshListener { refreshToUpdateImage() }

        //确定下载按钮
        sureInSelectPicture_Button.setOnClickListener {
            //先检查有无选择图片
            if (selectImageUriList.isEmpty())  {
                "请选择图片后再下载".toastShort()
                return@setOnClickListener
            }

            //下载原图的进度提示框
            downloadOriginalImageDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                titleText = "正在下载图片回本地"
                setCancelable(false)
                progressHelper.barColor = getColor(R.color.dracula_bottom_toolbar_apply)
                show()
            }

            //通过用户选择图片的uri解析得到对应object的文件名
            for (uri in selectImageUriList) {
                selectImageNameList.add(uri.getStrByIndex('/',1).getStrByIndex('.'))
            }
            selectImageUriList.clear()
            ImageAsyncTask(ImageAsyncTask.DOWNLOAD_ORIGINAL_TASK).execute()

            //将所有的CheckBox都设成未勾选
            for (i in 0 until imagePathForAdapterList.size) checkStatus[i] = false
            imageForSelectAdapter.notifyDataSetChanged()
            alreadySelectNumber = 0
            showSelectText()
            selectAllImage_Button.text = "全选"
            selectAllImage_Button.setTextColor(getColor(R.color.selectAllColor))
        }
        //全选按钮
        selectAllImage_Button.setOnClickListener {
            //没有全选的情况，就选上所有图片
            if (selectAllImage_Button.text.length == 2) {
                //按钮变化
                selectAllImage_Button.text = "全不选"
                selectAllImage_Button.setTextColor(getColor(R.color.buttonBlue))

                //如果lastNumber等于0说明已经加载完，直接已选数设成全部的图片数，否则设成已加载的图片数
                alreadySelectNumber = imagePathForAdapterList.size
                showSelectText()

                //将所有的CheckBox都设成已勾选
                for (i in 0 until alreadySelectNumber) checkStatus[i] = true

                //将当前已加载的所有图片uri都放进去
                selectImageUriList.addAll(imagePathForAdapterList)
                imageForSelectAdapter.notifyDataSetChanged()
            } else {
                //“全不选”的情况
                selectAllImage_Button.text = "全选"
                selectAllImage_Button.setTextColor(getColor(R.color.selectAllColor))

                alreadySelectNumber = 0
                showSelectText()

                //将所有的CheckBox都设成未勾选
                for (i in 0 until imagePathForAdapterList.size) checkStatus[i] = false

                selectImageUriList.clear()
                imageForSelectAdapter.notifyDataSetChanged()
            }
        }
        //一键上划的按钮
        scrollToTop_Button.setOnClickListener { imageForChoose_RecyclerView.smoothScrollToPosition(0) }

        //返回按钮
        backInSelectPicture_Button.setOnClickListener { this.finish() }
    }

    //下拉加载事件
    private fun refreshToUpdateImage() {
        when {
            canRefresh -> {
                downloadStart = imagePathForAdapterList.size + 1
                downloadEnd = min(downloadStart + 20, objectNumberInTotal)
                ImageAsyncTask(ImageAsyncTask.DOWNLOAD_REFRESH_TASK).execute()
                "发现${objectNumberInTotal-downloadStart+1}张新图片,正在下载".toastShort()
                canRefresh = false
            }
            //已经加载完了，设置一下没有更多数据了
            imagePathForAdapterList.size == objectNumberInTotal -> {
                imageForChoose_RefreshLayout.finishRefreshWithNoMoreData()
                "到底啦".toastShort()
                Handler().postDelayed({imageForChoose_RecyclerView.smoothScrollToPosition(imagePathForAdapterList.size)},430)
            }
            //没有加载完，但是超过了21张图片，就自动拉到底
            imagePathForAdapterList.size > 21 -> {
                imageForChoose_RefreshLayout.finishRefresh()
                "已定位到上次加载的位置，仍有图片可显示".toastShort()
                Handler().postDelayed({imageForChoose_RecyclerView.smoothScrollToPosition(imagePathForAdapterList.size)},430)
            }
            else -> imageForChoose_RefreshLayout.finishRefresh()
        }
    }

    //上拉刷新事件
    private fun downloadMoreImage() {
        canRefresh = false
        //已经加载完，这一次已不能加载
        if (imagePathForAdapterList.size == objectNumberInTotal) {
            imageForChoose_RefreshLayout.finishLoadMore()
            imageForChoose_RefreshLayout.setNoMoreData(true)
        } else {
            downloadStart = imagePathForAdapterList.size + 1
            //情况一：这次加载完后就不能在加载了
            if (downloadStart + moreDownloadNumber > objectNumberInTotal) {
                //将下载的最后一个限制在objectNumber中
                downloadEnd = objectNumberInTotal
                ImageAsyncTask(ImageAsyncTask.DOWNLOAD_MORE_COMPRESS_TASK).execute()
            }
            //情况二：这次加载完后还可以继续加载
            else {
                downloadEnd += moreDownloadNumber
                ImageAsyncTask(ImageAsyncTask.DOWNLOAD_MORE_COMPRESS_TASK).execute()
            }
        }
    }

    //recyclerview的适配器
    inner class ImageAdapter : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

        private var imageAdapterContext: Context ?=null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageAdapter.ViewHolder {
            if (imageAdapterContext == null) imageAdapterContext = parent.context
            val view = LayoutInflater.from(imageAdapterContext).inflate(R.layout.image_select, parent, false)
            val holder = ViewHolder(view)
            holder.cardView.setOnClickListener {
                //点击某个图片的事件，可以放大查看图片
                val position: Int = holder.adapterPosition
                val clickPictureUri: String = imagePathForAdapterList[position]
                //新开一个activity来放大图片
                view.context.startActivity(Intent(view.context, LargerImage::class.java).putExtra("imageUri", clickPictureUri),  // 注意这里的sharedView
                    // Content，View（动画作用view），String（和XML一样）
                    ActivityOptions.makeSceneTransitionAnimation(view.context as Activity, view, "sharedView").toBundle())
            }
            return holder
        }

        override fun onBindViewHolder(holder: ImageAdapter.ViewHolder, position: Int) {
            val imageUri: String = imagePathForAdapterList[position]
            imageAdapterContext?.let { Glide.with(it).load(imageUri).into(holder.imageView) }
            //极其重要的一句话，否则recyclerview会发生致命的混乱错误
            holder.checkBox.setOnCheckedChangeListener(null)
            //显示CheckBox的情况
            holder.checkBox.isChecked = checkStatus[position] ?:false
            //CheckBox发生变化时
            holder.checkBox.setOnCheckedChangeListener { _, b ->
                //checkStatus记录变化
                checkStatus[position] = b
                //变为勾选
                if (b) { selectImageUriList.add(imageUri)
                    //SelectPicture中“已选”文字变化
                    setAlreadySelectNumber(true)
                    //底部栏浮出
                    selectBottomAppBar.animate().translationY(0f)
                } else {
                    selectImageUriList.remove(imageUri)
                    setAlreadySelectNumber(false)
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var cardView: CardView = view as CardView
            var imageView: ImageView = view.findViewById(R.id.singleImageForChoose_ImageView)
            var checkBox: CheckBox = view.findViewById(R.id.singleCheckBoxForChoose)
        }

        override fun getItemCount() = imagePathForAdapterList.size
    }

    //用于CheckBox发生变化时记录已选择图片数量的变化
    fun setAlreadySelectNumber(choice: Boolean) {
        if (choice) alreadySelectNumber++
        else alreadySelectNumber--
        if(alreadySelectNumber == 0) selectBottomAppBar.animate().translationY(200f)
        showSelectText()
    }

    //显示已选择的图片数
    private fun showSelectText() {
        val selectString = alreadySelectNumber.toString()
        if (alreadySelectNumber != 0) sureInSelectPicture_Button.text = resources.getString(R.string.alreadySelectHint, " ",selectString," ")
        else sureInSelectPicture_Button.text=  "确定"
    }

    override fun onDestroy() {
        super.onDestroy()
        //清理选择状态
        checkStatus.clear()
        alreadySelectNumber = 0
        selectImageNameList.clear()
        selectImageUriList.clear()
    }
}
