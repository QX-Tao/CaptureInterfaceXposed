package com.android.captureinterfacexposed.ui.activity

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityOptionsCompat
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.databinding.ActivityDetailBinding
import com.android.captureinterfacexposed.db.PageDataHelper
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.SizeUtils
import java.io.File
import java.util.*
import kotlin.properties.Delegates

class DetailActivity : BaseActivity<ActivityDetailBinding>() {
    companion object{
        private lateinit var pageDataHelper: PageDataHelper
        private lateinit var loadingDialog: ProgressDialog
        private var detailList: List<DetailItem>? = null
        private var mid by Delegates.notNull<Long>()
    }
    private lateinit var pageCollectNum: String
    private lateinit var pageCollectData: String
    private lateinit var pkgName: String
    private var isDeleteData: Boolean = false
    private var isMultiSelectMode = false
    private var selectedItems = mutableSetOf<Int>()
    private lateinit var detailAdapter: DetailListAdapter


    override fun onCreate() {
        mid = intent.getLongExtra("mid",-1)
        pageCollectNum = intent.getStringExtra("page_collect_num").toString()
        pageCollectData = intent.getStringExtra("page_collect_data").toString()
        pkgName = intent.getStringExtra("pkgName").toString()

        binding.includeTitleBarSecond.tvTitle.text = pageCollectData
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener { onBackPressed() }
        binding.includeTitleBarSecond.ivMoreButton.setOnClickListener { showPopupMenu(binding.includeTitleBarSecond.ivMoreButton) }
        pageDataHelper = PageDataHelper(this)

        loadingDialog = ProgressDialog.show(this@DetailActivity,resources.getString(R.string.load_data_title), resources.getString(R.string.load_data_desc), true, false)
        LoadDataTask(1).execute()

        binding.includeTitleBarOperate.ivBackButton.setOnClickListener {
            selectedItems.clear()
            isMultiSelectMode = false
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            detailAdapter.notifyDataSetChanged()
        }
        binding.includeTitleBarOperate.ivCheckAll.setOnClickListener {
            if(selectedItems.size < detailList!!.size){
                selectedItems.clear()
                for (i in detailList!!.indices) {
                    selectedItems.add(i)
                }
                detailAdapter.notifyDataSetChanged()
            }
            val tmp2 = resources.getString(R.string.select_num)
            binding.includeTitleBarOperate.tvTitle.text = String.format(tmp2,selectedItems.size)
        }
        binding.includeTitleBarOperate.ivCheckInvert.setOnClickListener {
            val newSelectedItems = mutableSetOf<Int>()
            for (i in detailList!!.indices) {
                if (!selectedItems.contains(i)) {
                    newSelectedItems.add(i)
                }
            }
            selectedItems = newSelectedItems
            val tmp2 = resources.getString(R.string.select_num)
            binding.includeTitleBarOperate.tvTitle.text = String.format(tmp2,selectedItems.size)
            if(selectedItems.size == 0){
                selectedItems.clear()
                isMultiSelectMode = false
                binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
                binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            }
            detailAdapter.notifyDataSetChanged()
        }
        binding.includeTitleBarOperate.ivCheckDelete.setOnClickListener {
            loadingDialog = ProgressDialog.show(this@DetailActivity,resources.getString(R.string.processing_title), resources.getString(R.string.processing_desc), true, false)
            LoadDataTask(2).execute()
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            isDeleteData = true
        }

    }

    private inner class LoadDataTask(private val taskType: Int) : AsyncTask<Void?, Void?, Void?>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): Void? {
            when(taskType){
                1 -> processData()
                2 -> {
                    val itemsToRemove = mutableListOf<String?>()
                    selectedItems.forEach {
                        itemsToRemove.add(detailList!![it].nameId.toString())
                    }
                    itemsToRemove.forEach {
                        if(it != null) delDataDetail(it)
                    }
                    renameDetail()
                    processData()
                }
            }
            return null
        }
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(aVoid: Void?) {
            when(taskType){
                1 -> {
                    detailAdapter = DetailListAdapter(detailList!!)
                    binding.detailListView.adapter = detailAdapter
                    loadingDialog.dismiss() // 关闭进度条
                }
                2 -> {
                    detailAdapter = DetailListAdapter(detailList!!)
                    binding.detailListView.adapter = detailAdapter
                    loadingDialog.dismiss() // 关闭进度条
                    Toast.makeText(applicationContext,resources.getString(R.string.data_deleted), Toast.LENGTH_SHORT).show()
                    selectedItems.clear()
                    isMultiSelectMode = false
                }
            }
        }
    }

    fun processData(){
        detailList = getDetailItemList()
    }

    /**
     * get detailItemList
     *
     * 获取detailItemList
     */
    private fun getDetailItemList(): List<DetailItem> {
        val detailItems = mutableListOf<DetailItem>()
        for(i in 1 until pageCollectNum.toInt() + 1){
            val accessibilityFileName = "无障碍_TreeView($i).json"
            val sdkFileName = "SDK_TreeView($i).json"
            val screenFileName = "Screen($i).png"
            val tmpAccessibilityText = FileIOUtils.readFile2String(getFilePath(accessibilityFileName))
            val tmpSdkText = FileIOUtils.readFile2String(getFilePath(sdkFileName))
            val screenSize = ImageUtils.getSize(getFilePath(screenFileName))
            val outWidth = screenSize[0]
            val outHeight = screenSize[1]
            val ratio = outHeight.toFloat() / outWidth.toFloat()
            val tmpScreen = ImageUtils.scale(ImageUtils.getBitmap(getFilePath(screenFileName)),
                SizeUtils.dp2px(100F),SizeUtils.dp2px(100F * ratio))
            val screen = ImageUtils.bitmap2Drawable(tmpScreen)
            val accessibilityText =  if (tmpAccessibilityText.length > 320) tmpAccessibilityText.substring(0..320) else tmpAccessibilityText
            val sdkText = if (tmpSdkText.length > 320) tmpSdkText.substring(0..320) else tmpSdkText
            val detailItem = DetailItem(i, accessibilityFileName, sdkFileName, screenFileName,accessibilityText,sdkText,screen)
            detailItems.add(detailItem)
        }
        return detailItems
    }

    /**
     * del dataDetail by nameId
     *
     * 删除本地记录
     */
    private fun delDataDetail(nameId: String){
        val accessibilityFileName = "无障碍_TreeView($nameId).json"
        val sdkFileName = "SDK_TreeView($nameId).json"
        val screenFileName = "Screen($nameId).png"
        var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder) + File.separator + pkgName + File.separator + pageCollectData)
        if(filePath.exists() && filePath.isDirectory){
            val files = filePath.listFiles()
            for (file in files!!) {
                if (file.name == accessibilityFileName || file.name == sdkFileName || file.name == screenFileName) {
                    file.delete()
                }
            }
        }
    }

    /**
     * rename dataDetail after del
     *
     * 重命名数据
     */
    private fun renameDetail(){
        var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder) + File.separator + pkgName + File.separator + pageCollectData)
        val pageCollectItems = getFileNames(filePath.toString())
        pageCollectNum = (pageCollectItems.size / 3).toString()
        if(pageCollectNum.toInt() == 0){
            pageDataHelper.deleteCollectRow(mid,pageCollectData)
            delPageCollectData(pkgName,pageCollectData)
            return
        }
        val accessibilityName = mutableListOf<String>()
        val sdkName = mutableListOf<String>()
        val screenName = mutableListOf<String>()
        for (i in pageCollectItems.indices){
            if(pageCollectItems[i].contains("无障碍_TreeView")){
                accessibilityName.add(pageCollectItems[i])
            } else if (pageCollectItems[i].contains("SDK_TreeView")){
                sdkName.add(pageCollectItems[i])
            } else if (pageCollectItems[i].contains("Screen")){
                screenName.add(pageCollectItems[i])
            }
        }
        accessibilityName.sort()
        sdkName.sort()
        screenName.sort()
        for (i in 1 .. pageCollectNum.toInt()){
            val accessibilityFileName = "无障碍_TreeView($i).json"
            val sdkFileName = "SDK_TreeView($i).json"
            val screenFileName = "Screen($i).png"
            FileUtils.rename(filePath.toString() + File.separator + accessibilityName[i - 1],accessibilityFileName)
            FileUtils.rename(filePath.toString() + File.separator + sdkName[i - 1],sdkFileName)
            FileUtils.rename(filePath.toString() + File.separator + screenName[i - 1],screenFileName)
        }
        pageDataHelper.updatePageCollectNumByIdAndData(mid,pageCollectData,pageCollectNum.toInt())
    }

    /**
     * get file name
     *
     * 指定目录下的文件名称
     */
    private fun getFileNames(path: String): List<String> {
        val dir = File(path)
        return dir.listFiles { file -> file.isFile }?.map { file -> file.name } ?: emptyList()
    }

    /**
     * get filePath
     *
     * 获取路径
     */
    private fun getFilePath(fileName: String?): String{
        var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder) + File.separator + pkgName + File.separator + pageCollectData)
        return if(fileName.isNullOrBlank()) ""
        else "$filePath" + File.separator + fileName
    }

    /**
     * del pageCollectData by pkgName and collectData
     *
     * 删除本地记录
     */
    private fun delPageCollectData(pkgName: String, collectData : String){
        var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder) + File.separator + pkgName)
        if (filePath.exists() && filePath.isDirectory) {
            val files = filePath.listFiles()
            for (file in files!!) {
                if (file.name == collectData) {
                    deleteRecursive(file)
                    break
                }
            }
        }
    }
    /**
     * recursive deletion
     *
     * 递归删除
     */
    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (f in files) {
                    deleteRecursive(f)
                }
            }
        }
        file.delete()
    }

    /**
     * page collect item list adapter
     *
     * 列表适配器
     */
    private inner class DetailListAdapter(private val detailItemList: List<DetailItem>): BaseAdapter() {
        override fun getCount(): Int {
            return detailItemList.size
        }

        override fun getItem(position: Int): Any {
            return detailItemList[position].toString()
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var view = convertView
            val holder: ViewHolder
            if(view == null){
                view = LayoutInflater.from(applicationContext).inflate(R.layout.detail_list_item,parent,false)
                holder = ViewHolder(view)
                view.tag = holder
            } else{
                holder = view.tag as ViewHolder
            }
            val index = detailItemList[position]

            holder.accessibilityDescText.text = String.format(getString(R.string.accessibility_collects), index.accessibilityFileName)
            holder.sdkDescText.text = String.format(getString(R.string.sdk_collects), index.sdkFileName)
            holder.screen.setImageDrawable(index.screenDrawable)
            holder.accessibilityText.text =  index.accessibilityText
            holder.sdkText.text =  index.sdkText

            holder.accessibilityText.setTextColor(resources.getColor(R.color.firstTextColor))
            holder.sdkText.setTextColor(resources.getColor(R.color.firstTextColor))
            holder.accessibilityDescText.setTextColor(resources.getColor(R.color.secondTextColor))
            holder.sdkDescText.setTextColor(resources.getColor(R.color.secondTextColor))

            view?.setBackgroundResource(R.drawable.bg_ripple)
            view?.setOnLongClickListener {
                if (!isMultiSelectMode) {
                    enterMultiSelectMode(position)
                    return@setOnLongClickListener true
                }
                return@setOnLongClickListener false
            }
            view?.setOnClickListener {
                if (isMultiSelectMode){
                    enterMultiSelectMode(position)
                } else {
                    binding.detailListView
                }
            }
            holder.screen.setOnClickListener {
                val screenPath = getFilePath(Companion.detailList?.get(position)?.screenFileName)
                val intent = Intent(applicationContext,PhotoViewActivity::class.java)
                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this@DetailActivity, it, "shareElement").toBundle()
                intent.putExtra("screenPath",screenPath)
                startActivity(intent, bundle)
            }
            // 如果处于多选状态，则根据选中状态设置背景颜色
            if (isMultiSelectMode) {
                if (selectedItems.contains(position)) {
                    view?.setBackgroundColor(resources.getColor(R.color.coverColor))
                } else {
                    view?.setBackgroundResource(0)
                }
            }
            return view
        }

        private inner class ViewHolder(view: View) {
            val screen: ImageView = view.findViewById(R.id.iv_screen)
            val accessibilityText: TextView = view.findViewById(R.id.accessibility_text_view)
            val sdkText: TextView = view.findViewById(R.id.sdk_text_view)
            val accessibilityDescText: TextView = view.findViewById(R.id.accessibility_desc_text_view)
            val sdkDescText: TextView = view.findViewById(R.id.sdk_desc_text_view)
        }
    }

    /**
     * enter multiselect mode
     *
     * 多选模式
     */
    private fun enterMultiSelectMode(position: Int) {
        isMultiSelectMode = true
        binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.GONE
        binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.VISIBLE
        // 切换选中状态
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        val tmp = resources.getString(R.string.select_num)
        binding.includeTitleBarOperate.tvTitle.text = String.format(tmp,selectedItems.size)
        if(selectedItems.size == 0){ // 未选中某一项 则退出MultiSelectMode
            isMultiSelectMode = false
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
        }
        // 刷新列表项
        val adapter = binding.detailListView.adapter as DetailListAdapter
        adapter.notifyDataSetChanged()
    }

    /**
     * show popup menu
     *
     * 弹出菜单
     */
    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_popup, popupMenu.menu)
        popupMenu.menu.findItem(R.id.restart_sys).isVisible = false
        popupMenu.menu.findItem(R.id.restart_app).isVisible = false
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == R.id.refresh_list) {
                detailList = null
                binding.detailListView.adapter = null
                loadingDialog = ProgressDialog.show(this@DetailActivity,resources.getString(R.string.load_data_title), resources.getString(R.string.load_data_desc), true, false)
                LoadDataTask(1).execute()
            }
            true
        }
        popupMenu.show()
    }

    /**
     * detail item class
     *
     * DetailItem类
     */
    private class DetailItem(
        val nameId: Int,
        val accessibilityFileName: String?,
        val sdkFileName: String,
        val screenFileName: String,
        val accessibilityText: String,
        val sdkText: String,
        val screenDrawable: Drawable
    ) {
        override fun toString(): String {
            return """
                    accessibilityFileName: $accessibilityFileName
                    sdkFileName: $sdkFileName
                    screenFileName: $screenFileName
                    """.trimIndent()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(isMultiSelectMode){
            selectedItems.clear()
            isMultiSelectMode = false
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            detailAdapter.notifyDataSetChanged()
        } else {
            val resultIntent = Intent()
            resultIntent.putExtra("isDeleteData", isDeleteData)
            setResult(RESULT_OK, resultIntent)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pageDataHelper.close()
    }
}