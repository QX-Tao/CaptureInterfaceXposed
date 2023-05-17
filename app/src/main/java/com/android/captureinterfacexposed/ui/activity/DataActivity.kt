package com.android.captureinterfacexposed.ui.activity

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.databinding.ActivityDataBinding
import com.android.captureinterfacexposed.db.PageDataHelper
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import com.blankj.utilcode.util.ZipUtils
import java.io.File

class DataActivity : BaseActivity<ActivityDataBinding>() {
    companion object{
        private lateinit var pageDataHelper: PageDataHelper
        private lateinit var loadingDialog: ProgressDialog
        private var pageItemList: List<PageItem>? = null
        private var pageTmpList:  List<PageDataHelper.Page>? = null
    }
    private var selectedItems = mutableSetOf<Int>()
    private var isMultiSelectMode = false
    private lateinit var pageItemAdapter: PageItemListAdapter
    private lateinit var filePath1: File
    private val zipFileNames = mutableListOf<String>()

    override fun onCreate() {
        binding.includeTitleBarSecond.tvTitle.text = getString(R.string.collect_result)
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener { onBackPressed() }
        binding.includeTitleBarSecond.ivMoreButton.setOnClickListener { showPopupMenu(binding.includeTitleBarSecond.ivMoreButton) }
        pageDataHelper = PageDataHelper(this)
        loadingDialog = ProgressDialog.show(this@DataActivity,resources.getString(R.string.load_data_title), resources.getString(R.string.load_data_desc), true, false)
        LoadDataTask(1).execute()
        binding.includeTitleBarOperate.ivBackButton.setOnClickListener {
            selectedItems.clear()
            isMultiSelectMode = false
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            pageItemAdapter.notifyDataSetChanged()
        }
        binding.includeTitleBarOperate.ivCheckAll.setOnClickListener {
            if(selectedItems.size < pageItemList!!.size){
                selectedItems.clear()
                for (i in pageItemList!!.indices) {
                    selectedItems.add(i)
                }
                pageItemAdapter.notifyDataSetChanged()
            }
            val tmp = resources.getString(R.string.select_num)
            binding.includeTitleBarOperate.tvTitle.text = String.format(tmp,selectedItems.size)
        }
        binding.includeTitleBarOperate.ivCheckInvert.setOnClickListener {
            val newSelectedItems = mutableSetOf<Int>()
            for (i in pageItemList!!.indices) {
                if (!selectedItems.contains(i)) {
                    newSelectedItems.add(i)
                }
            }
            selectedItems = newSelectedItems
            val tmp = resources.getString(R.string.select_num)
            binding.includeTitleBarOperate.tvTitle.text = String.format(tmp,selectedItems.size)
            if(selectedItems.size == 0){
                selectedItems.clear()
                isMultiSelectMode = false
                binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
                binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            }
            pageItemAdapter.notifyDataSetChanged()
        }
        binding.includeTitleBarOperate.ivCheckDelete.setOnClickListener {
            loadingDialog = ProgressDialog.show(this@DataActivity,resources.getString(R.string.processing_title), resources.getString(R.string.processing_desc), true, false)
            LoadDataTask(3).execute()
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
        }
        binding.btExportData.setOnClickListener {
            zipFileNames.clear()
            filePath1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            filePath1 = File(filePath1.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder))
            if (isMultiSelectMode){
                selectedItems.forEach {
                    val zipTmpFileName = pageItemList!![it].packageName
                    zipFileNames.add(filePath1.toString() + File.separator + zipTmpFileName)
                }
            } else {
                for (i in pageItemList!!.indices){
                    val zipTmpFileName = pageItemList!![i].packageName
                    zipFileNames.add(filePath1.toString() + File.separator + zipTmpFileName)
                }
            }
            loadingDialog = ProgressDialog.show(this@DataActivity,resources.getString(R.string.processing_title), resources.getString(R.string.processing_desc), true, false)
            LoadDataTask(2).execute()
        }
    }

    private inner class LoadDataTask(private val taskType: Int) : AsyncTask<Void?, Void?, Void?>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): Void? {
            when(taskType){
                1 -> {
                    syncData()
                    processData()
                }
                2 -> ZipUtils.zipFiles(zipFileNames, "$filePath1.zip")
                3 -> {
                    val itemsToRemove = mutableListOf<Long>()
                    val itemsToRemovePkgName = mutableListOf<String>()
                    selectedItems.forEach {
                        itemsToRemove.add(pageItemList!![it].mid)
                        itemsToRemovePkgName.add(pageItemList!![it].packageName)
                    }
                    itemsToRemove.forEach { pageDataHelper.delPageAndCollectData(it) }
                    itemsToRemovePkgName.forEach{ delPageData(it) }
                    processData()
                }
            }
            return null
        }
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(aVoid: Void?) {
            when(taskType) {
                1 -> {
                    pageItemAdapter = PageItemListAdapter(pageItemList!!)
                    binding.pageItemListView.adapter = pageItemAdapter
                    loadingDialog.dismiss() // 关闭进度条
                }
                2 -> {
                    loadingDialog.dismiss() // 关闭进度条
                    Toast.makeText(applicationContext,resources.getString(R.string.data_exported),Toast.LENGTH_SHORT).show()
                    selectedItems.clear()
                    isMultiSelectMode = false
                    binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
                    binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
                    pageItemAdapter.notifyDataSetChanged()
                }
                3 -> {
                    pageItemAdapter = PageItemListAdapter(pageItemList!!)
                    binding.pageItemListView.adapter = pageItemAdapter
                    loadingDialog.dismiss() // 关闭进度条
                    Toast.makeText(applicationContext,resources.getString(R.string.data_deleted),Toast.LENGTH_SHORT).show()
                    selectedItems.clear()
                    isMultiSelectMode = false
                }
            }
        }
    }

    /**
     * process data
     *
     * 拉取数据
     */
    fun processData(){
        pageTmpList = pageDataHelper.allPages
        pageItemList = getPageItemList(pageTmpList!!)
    }

    /**
     * page item list adapter
     *
     * 列表适配器
     */
    private inner class PageItemListAdapter(private val pageItemList: List<PageItem>): BaseAdapter() {
        override fun getCount(): Int {
            return pageItemList.size
        }
        override fun getItem(position: Int): Any {
            return pageItemList[position].toString()
        }
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var view = convertView
            val holder: ViewHolder
            if(view == null){
                view = LayoutInflater.from(applicationContext).inflate(R.layout.data_list_item,parent,false)
                holder = ViewHolder(view)
                view.tag = holder
            } else{
                holder = view.tag as ViewHolder
            }
            val index = pageItemList[position]
            if(index.appIcon == null){
                holder.appIcon.setImageResource(R.drawable.ic_default_apk)
            } else {
                holder.appIcon.setImageDrawable(index.appIcon)
            }
            holder.appName.text = index.appName
            holder.packageName.text = index.packageName
            val tmp = resources.getString(R.string.page_copies)
            holder.pageNum.text = String.format(tmp,index.pageNum)
            holder.appName.setTextColor(resources.getColor(R.color.firstTextColor))
            holder.packageName.setTextColor(resources.getColor(R.color.secondTextColor))
            holder.pageNum.setTextColor(resources.getColor(R.color.thirdTextColor))
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
                    val appName = Companion.pageItemList?.get(position)?.appName
                    val pkgName = Companion.pageItemList?.get(position)?.packageName
                    val mid = Companion.pageItemList?.get(position)?.mid
                    val intent = Intent(applicationContext,InfoActivity::class.java)
                    intent.putExtra("app_name", appName)
                    intent.putExtra("package_name", pkgName)
                    intent.putExtra("mid",mid)
                    startActivity(intent)
                }
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
            val appIcon: ImageView = view.findViewById(R.id.app_icon_image_view)
            val appName: TextView = view.findViewById(R.id.app_name_text_view)
            val packageName: TextView = view.findViewById(R.id.package_name_text_view)
            val pageNum: TextView = view.findViewById(R.id.page_num_text_view)
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
        val adapter = binding.pageItemListView.adapter as PageItemListAdapter
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
                pageItemList = null
                pageTmpList = null
                binding.pageItemListView.adapter = null
                loadingDialog = ProgressDialog.show(this@DataActivity,resources.getString(R.string.load_data_title), resources.getString(R.string.load_data_desc), true, false)
                LoadDataTask(1).execute()
            }
            true
        }
        popupMenu.show()
    }

    /**
     * get app icon by its packageName
     *
     * 获取应用图标
     */
    private fun getIconByPkgName(packageName:String): Drawable? {
        val packageManager = applicationContext.packageManager
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationIcon(appInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * get pageItemList
     *
     * 获取pageItemList
     */
    private fun getPageItemList(pages:List<PageDataHelper.Page>): List<PageItem> {
        val pageItems = mutableListOf<PageItem>()
        for (i in pages.indices){
            val packageName = pages[i].packageName
            val pageNum = pages[i].pageNum
            val index = pages[i].mid
            val appName = pages[i].appName
            val appIcon = getIconByPkgName(packageName)
            val pageItem = PageItem(appName,packageName,pageNum,appIcon,index)
            pageItems.add(pageItem)
        }
        return pageItems
    }

    /**
     * del pageData by pkgName
     *
     * 删除本地记录
     */
    private fun delPageData(pkgName: String){
        var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder))
        if (filePath.exists() && filePath.isDirectory) {
            val files = filePath.listFiles()
            for (file in files) {
                if (file.name == pkgName) {
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
     * page item class
     *
     * pageItem类
     */
    private class PageItem(
        val appName: String?,
        val packageName: String,
        val pageNum: String,
        val appIcon: Drawable?,
        val mid: Long
    ) {
        override fun toString(): String {
            return """
                    mid: $mid
                    appName: $appName
                    packageName: $packageName
                    pageNum: $pageNum
                    """.trimIndent()
        }
    }

    /**
     * synchronize data between database and local
     *
     * 同步数据
     */
    private fun syncData() {
        val mDbHelper = PageDataHelper(applicationContext)
        mDbHelper.clearDatabase()
        var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        when(newDirectory(filePath.toString(), applicationContext.resources.getString(R.string.collect_folder))){
            true -> return
            false -> {
                filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder))
                val subdirectories = getSubdirectories(filePath.toString()) // appName subdirectories -> pkgName
                if (subdirectories.isEmpty()) return
                subdirectories.forEach { it1 -> // pkgName
                    val filePath1 = File(filePath.toString() + File.separator + it1)
                    val subdirectories1 = getSubdirectories(filePath1.toString()) // pkgName subdirectories -> collectTime
                    if (subdirectories1.isEmpty()) return@forEach // pkgName/collectTime Empty -> return
                    val pageNum = subdirectories1.size // pkgName/collectTime num -> pageNum
                    val appName: String? = getAppNameByPkgName(it1)
                    val pageId = mDbHelper.addPage(it1, appName, pageNum) // add a page
                    subdirectories1.forEach{ it2 -> // CollectTime
                        val filePath2 = File(filePath1.toString() + File.separator + it2)
                        val pageCollectItems = getFileNames(filePath2.toString())
                        val pageCollectNum = pageCollectItems.size / 3
                        mDbHelper.addCollect(pageId,it2,pageCollectNum)
                    }
                }
            }
        }
        mDbHelper.close()
    }

    /**
     * new a directory
     *
     * 创建文件夹
     */
    private fun newDirectory(path: String, dirName: String):Boolean {
        val file = File("$path/$dirName")
        try {
            if (!file.exists()) {
                file.mkdirs()
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * get sub directories
     *
     * 指定文件夹下一级目录的文件夹列表
     */
    private fun getSubdirectories(path: String): List<String> {
        val dir = File(path)
        return dir.listFiles { file -> file.isDirectory }?.map { file -> file.name } ?: emptyList()
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
     * get app name by its packageName
     *
     * 获取应用名称
     */
    private fun getAppNameByPkgName(packageName: String): String? {
        val packageManager: PackageManager = applicationContext.packageManager
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        pageDataHelper.close()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(isMultiSelectMode){
            selectedItems.clear()
            isMultiSelectMode = false
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            pageItemAdapter.notifyDataSetChanged()
        } else {
            super.onBackPressed()
        }
    }
}