package com.android.captureinterfacexposed.ui.activity

import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.databinding.ActivityInfoBinding
import com.android.captureinterfacexposed.db.PageDataHelper
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import com.blankj.utilcode.util.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.properties.Delegates

class InfoActivity : BaseActivity<ActivityInfoBinding>(){
    companion object{
        private lateinit var pageDataHelper: PageDataHelper
        private var mid by Delegates.notNull<Long>()
        private var pageCollectItemList: List<PageCollectItem>? = null
        private var pageTmpCollectList: List<PageDataHelper.PageCollect>? = null
        @JvmStatic
        private val INFO_REQUEST_CODE = 100
    }
    private var selectedItems = mutableSetOf<Int>()
    private var isMultiSelectMode = false
    private var isProcessBarStatus = false
    private lateinit var pageCollectItemAdapter: PageCollectItemListAdapter
    private lateinit var filePath1: File
    private val zipFileNames = mutableListOf<String>()
    private lateinit var appName: String
    private lateinit var pkgName: String
    private var isDeleteData: Boolean = false

    override fun onCreate() {
        appName = intent.getStringExtra("app_name").toString()
        pkgName = intent.getStringExtra("package_name").toString()
        mid = intent.getLongExtra("mid",-1)

        val tmp1 = resources.getString(R.string.app_infos)
        binding.includeTitleBarSecond.tvTitle.text = String.format(tmp1,appName,pkgName)
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener { finish() }
        binding.includeTitleBarSecond.ivMoreButton.setOnClickListener { showPopupMenu(binding.includeTitleBarSecond.ivMoreButton) }

        pageDataHelper = PageDataHelper(this)
        lifecycleScope.launch { loadData() }


        binding.includeTitleBarOperate.ivBackButton.setOnClickListener {
            selectedItems.clear()
            isMultiSelectMode = false
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            pageCollectItemAdapter.notifyDataSetChanged()
        }
        binding.includeTitleBarOperate.ivCheckAll.setOnClickListener {
            if(selectedItems.size < pageCollectItemList!!.size){
                selectedItems.clear()
                for (i in pageCollectItemList!!.indices) {
                    selectedItems.add(i)
                }
                pageCollectItemAdapter.notifyDataSetChanged()
            }
            val tmp2 = resources.getString(R.string.select_num)
            binding.includeTitleBarOperate.tvTitle.text = String.format(tmp2,selectedItems.size)
        }
        binding.includeTitleBarOperate.ivCheckInvert.setOnClickListener {
            val newSelectedItems = mutableSetOf<Int>()
            for (i in pageCollectItemList!!.indices) {
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
            pageCollectItemAdapter.notifyDataSetChanged()
        }
        binding.includeTitleBarOperate.ivCheckDelete.setOnClickListener {
            lifecycleScope.launch { delData() }
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            isDeleteData = true
        }
        binding.btExportData.setOnClickListener {
            zipFileNames.clear()
            filePath1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            filePath1 = File(filePath1.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder) + File.separator + pkgName)
            if (isMultiSelectMode){
                selectedItems.forEach {
                    val zipTmpFileName = pageCollectItemList!![it].pageCollectData ?: ""
                    zipFileNames.add(filePath1.toString() + File.separator + zipTmpFileName)
                }
            } else {
                for (i in pageCollectItemList!!.indices){
                    val zipTmpFileName = pageCollectItemList!![i].pageCollectData ?: ""
                    zipFileNames.add(filePath1.toString() + File.separator + zipTmpFileName)
                }
            }
            lifecycleScope.launch { exportData() }
        }
        onBackPressedDispatcher.addCallback(
            this, // lifecycle owner
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!isProcessBarStatus) {
                        if(isMultiSelectMode){
                            selectedItems.clear()
                            isMultiSelectMode = false
                            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
                            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
                            pageCollectItemAdapter.notifyDataSetChanged()
                        } else {
                            val resultIntent = Intent()
                            resultIntent.putExtra("isDeleteData", isDeleteData)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                }
            })
    }

    private fun processData(){
        pageTmpCollectList = pageDataHelper.getPageCollectsByMid(mid)
        pageCollectItemList = getPageCollectItemList(pageTmpCollectList!!)
    }

    /**
     * page collect item list adapter
     *
     * 列表适配器
     */
    private inner class PageCollectItemListAdapter(private val pageCollectItemList: List<PageCollectItem>): BaseAdapter() {
        override fun getCount(): Int {
            return pageCollectItemList.size
        }

        override fun getItem(position: Int): Any {
            return pageCollectItemList[position].toString()
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var view = convertView
            val holder: ViewHolder
            if(view == null){
                view = LayoutInflater.from(applicationContext).inflate(R.layout.info_list_item,parent,false)
                holder = ViewHolder(view)
                view.tag = holder
            } else{
                holder = view.tag as ViewHolder
            }
            val index = pageCollectItemList[position]
            holder.pageCollectIcon.setImageDrawable(
                when(index.pageCollectNum){
                    "1" -> resources.getDrawable(R.drawable.ic_collect_page_less)
                    "2" -> resources.getDrawable(R.drawable.ic_collect_page_default)
                    else -> resources.getDrawable(R.drawable.ic_collect_page_more)
                }
            )
            holder.pageCollectData.text = index.pageCollectData

            val tmp = resources.getString(R.string.page_copies)
            holder.pageCollectNum.text = String.format(tmp,index.pageCollectNum)
            holder.pageCollectDesc.setTextColor(resources.getColor(R.color.secondTextColor))
            holder.pageCollectData.setTextColor(resources.getColor(R.color.firstTextColor))
            holder.pageCollectNum.setTextColor(resources.getColor(R.color.thirdTextColor))
            view?.setBackgroundResource(R.drawable.bg_ripple)
            view?.setOnLongClickListener {
                if (isProcessBarStatus)
                    return@setOnLongClickListener true
                if (!isMultiSelectMode) {
                    enterMultiSelectMode(position)
                    return@setOnLongClickListener true
                }
                return@setOnLongClickListener false
            }
            view?.setOnClickListener {
                if (isProcessBarStatus)
                    return@setOnClickListener
                if (isMultiSelectMode){
                    enterMultiSelectMode(position)
                } else {
                    val pageCollectNum = Companion.pageCollectItemList?.get(position)?.pageCollectNum
                    val pageCollectData = Companion.pageCollectItemList?.get(position)?.pageCollectData
                    val intent = Intent(applicationContext,DetailActivity::class.java)
                    intent.putExtra("mid",mid)
                    intent.putExtra("page_collect_num", pageCollectNum)
                    intent.putExtra("page_collect_data", pageCollectData)
                    intent.putExtra("pkgName",pkgName)
                    startActivityForResult(intent, INFO_REQUEST_CODE)
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
            val pageCollectIcon: ImageView = view.findViewById(R.id.collect_page_image_view)
            val pageCollectDesc: TextView = view.findViewById(R.id.collect_page_desc_text_view)
            val pageCollectData: TextView = view.findViewById(R.id.collect_page_data_text_view)
            val pageCollectNum: TextView = view.findViewById(R.id.collect_page_num_text_view)
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
        val adapter = binding.collectItemListView.adapter as PageCollectItemListAdapter
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
                pageCollectItemList = null
                pageTmpCollectList = null
                binding.collectItemListView.adapter = null
                lifecycleScope.launch { loadData() }
            }
            true
        }
        popupMenu.show()
    }

    /**
     * get pageCollectItemList
     *
     * 获取pageCollectItemList
     */
    private fun getPageCollectItemList(pageCollects:List<PageDataHelper.PageCollect>): List<PageCollectItem> {
        val pageCollectItems = mutableListOf<PageCollectItem>()
        for (i in pageCollects.indices){
            val pageCollectData = pageCollects[i].pageCollectData
            val pageCollectNum = pageCollects[i].pageCollectNum
            val index = mid
            val pageCollectItem = PageCollectItem(pageCollectData, pageCollectNum, index)
            pageCollectItems.add(pageCollectItem)
        }
        return pageCollectItems
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
     * del pageData by pkgName
     *
     * 删除本地记录
     */
    private fun delPageData(pkgName: String){
        var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.collect_folder))
        if (filePath.exists() && filePath.isDirectory) {
            val files = filePath.listFiles()
            for (file in files!!) {
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
     * page collect item class
     *
     * pageCollectItem类
     */
    private class PageCollectItem(
        val pageCollectData: String?,
        val pageCollectNum: String,
        val mid: Long
    ) {
        override fun toString(): String {
            return """
                    mid: $mid
                    pageCollectData: $pageCollectData
                    pageCollectNum: $pageCollectNum
                    """.trimIndent()
        }
    }

    /**
     * on activity result
     *
     * 结果回调
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INFO_REQUEST_CODE && resultCode == RESULT_OK) {
            val isDeleteData1 = data!!.getBooleanExtra("isDeleteData", false)
            if(isDeleteData1){
                pageCollectItemList = null
                pageTmpCollectList = null
                binding.collectItemListView.adapter = null
                lifecycleScope.launch { loadData() }
                isDeleteData = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pageDataHelper.close()
    }

    private suspend fun loadData() {
        inProcessBar()
        withContext(Dispatchers.IO) {
            processData()
        }
        outProcessBar()
        withContext(Dispatchers.Main) {
            pageCollectItemAdapter = PageCollectItemListAdapter(pageCollectItemList!!)
            binding.collectItemListView.adapter = pageCollectItemAdapter
        }
    }

    private suspend fun exportData() {
        inProcessBar()
        withContext(Dispatchers.IO) {
            ZipUtils.zipFiles(zipFileNames, "$filePath1.zip")
        }
        outProcessBar()
        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext,resources.getString(R.string.data_exported),Toast.LENGTH_SHORT).show()
            selectedItems.clear()
            isMultiSelectMode = false
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            pageCollectItemAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun delData() {
        inProcessBar()
        withContext(Dispatchers.IO) {                    val itemsToRemove = mutableListOf<String?>()
            selectedItems.forEach {
                itemsToRemove.add(pageCollectItemList!![it].pageCollectData)
            }
            itemsToRemove.forEach {
                pageDataHelper.deleteCollectRow(mid,it)
                pageDataHelper.decrementPageNumById(mid)
                if (pkgName.isNotBlank()) {
                    if (it != null) {
                        delPageCollectData(pkgName, it)
                    }
                }
            }
            if(pageDataHelper.getPageNumById(mid) == 0) {
                pageDataHelper.delPageAndCollectData(mid)
                if (pkgName.isNotBlank()) {
                    delPageData(pkgName)
                }
            }
            processData()
        }
        outProcessBar()
        withContext(Dispatchers.Main) {
            pageCollectItemAdapter = PageCollectItemListAdapter(pageCollectItemList!!)
            binding.collectItemListView.adapter = pageCollectItemAdapter
            Toast.makeText(applicationContext,resources.getString(R.string.data_deleted),Toast.LENGTH_SHORT).show()
            selectedItems.clear()
            isMultiSelectMode = false
        }
    }

    private fun inProcessBar(){
        isProcessBarStatus = true
        binding.progressBar.visibility = View.VISIBLE
        binding.btExportData.visibility = View.GONE
        binding.includeTitleBarSecond.ivBackButton.isEnabled = false
        binding.includeTitleBarSecond.ivMoreButton.isEnabled = false
        binding.includeTitleBarOperate.ivBackButton.isEnabled = false
        binding.includeTitleBarOperate.ivCheckAll.isEnabled = false
        binding.includeTitleBarOperate.ivCheckDelete.isEnabled = false
        binding.includeTitleBarOperate.ivCheckInvert.isEnabled = false
    }

    private fun outProcessBar(){
        isProcessBarStatus = false
        binding.progressBar.visibility = View.GONE
        binding.btExportData.visibility = View.VISIBLE
        binding.includeTitleBarSecond.ivBackButton.isEnabled = true
        binding.includeTitleBarSecond.ivMoreButton.isEnabled = true
        binding.includeTitleBarOperate.ivBackButton.isEnabled = true
        binding.includeTitleBarOperate.ivCheckAll.isEnabled = true
        binding.includeTitleBarOperate.ivCheckDelete.isEnabled = true
        binding.includeTitleBarOperate.ivCheckInvert.isEnabled = true
    }
}