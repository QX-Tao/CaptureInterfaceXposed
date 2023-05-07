package com.android.captureinterfacexposed.ui.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.databinding.ActivityInfoBinding
import com.android.captureinterfacexposed.db.PageDataHelper
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import java.io.File
import kotlin.properties.Delegates

class InfoActivity : BaseActivity<ActivityInfoBinding>(){
    companion object{
        private lateinit var pageDataHelper: PageDataHelper
        private lateinit var loadingDialog: ProgressDialog
        private var mid by Delegates.notNull<Long>()
        private var pageCollectItemList: List<PageCollectItem>? = null
        private var pageTmpCollectList: List<PageDataHelper.PageCollect>? = null
    }
    private var selectedItems = mutableSetOf<Int>()
    private var isMultiSelectMode = false
    private lateinit var pageCollectItemAdapter: PageCollectItemListAdapter

    override fun onCreate() {
        val appName = intent.getStringExtra("app_name")
        val pkgName = intent.getStringExtra("package_name")
        mid = intent.getLongExtra("mid",-1)

        binding.includeTitleBarSecond.tvTitle.text = appName + " (" + pkgName + ")"
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener { onBackPressed() }
        binding.includeTitleBarSecond.ivMoreButton.setOnClickListener { showPopupMenu(binding.includeTitleBarSecond.ivMoreButton) }

        pageDataHelper = PageDataHelper(this)
        loadingDialog = ProgressDialog.show(this@InfoActivity,"数据加载中", "请稍后...", true, false)
        LoadDataTask().execute()

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
            binding.includeTitleBarOperate.tvTitle.text = "已选" + selectedItems.size +"项"
        }
        binding.includeTitleBarOperate.ivCheckInvert.setOnClickListener {
            val newSelectedItems = mutableSetOf<Int>()
            for (i in pageCollectItemList!!.indices) {
                if (!selectedItems.contains(i)) {
                    newSelectedItems.add(i)
                }
            }
            selectedItems = newSelectedItems
            binding.includeTitleBarOperate.tvTitle.text = "已选" + selectedItems.size +"项"
            if(selectedItems.size == 0){
                selectedItems.clear()
                isMultiSelectMode = false
                binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
                binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
            }
            pageCollectItemAdapter.notifyDataSetChanged()
        }
        binding.includeTitleBarOperate.ivCheckDelete.setOnClickListener {
            val itemsToRemove = mutableListOf<String?>()
            selectedItems.forEach {
                itemsToRemove.add(pageCollectItemList!![it].pageCollectData)
            }
            itemsToRemove.forEach {
                pageDataHelper.deleteCollectRow(mid,it)
                pageDataHelper.decrementPageNumById(mid)
                if (pkgName != null) {
                    if (it != null) {
                        delPageCollectData(pkgName, it)
                    }
                }
            }
            if(pageDataHelper.getPageNumById(mid) == 0) {
                pageDataHelper.delPageAndCollectData(mid)
                if (pkgName != null) {
                    delPageData(pkgName)
                }
            }
            processData()
            pageCollectItemAdapter = PageCollectItemListAdapter(pageCollectItemList!!)
            binding.collectItemListView.adapter = pageCollectItemAdapter
            selectedItems.clear()
            isMultiSelectMode = false
            binding.includeTitleBarSecond.includeTitleBarSecond.visibility = View.VISIBLE
            binding.includeTitleBarOperate.includeTitleBarOperate.visibility = View.GONE
        }
    }

    private inner class LoadDataTask : AsyncTask<Void?, Void?, Void?>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): Void? {
            processData()
            return null
        }
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(aVoid: Void?) {
            pageCollectItemAdapter = PageCollectItemListAdapter(pageCollectItemList!!)
            binding.collectItemListView.adapter = pageCollectItemAdapter
            loadingDialog.dismiss() // 关闭进度条
        }
    }

    fun processData(){
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
            holder.pageCollectIcon.setImageResource(
                when(index.pageCollectNum){
                    "1" -> R.drawable.ic_collect_page_less
                    "2" -> R.drawable.ic_collect_page_default
                    else -> R.drawable.ic_collect_page_more
                }
            )
            holder.pageCollectData.text = index.pageCollectData
            holder.pageCollectNum.text = index.pageCollectNum + "份"
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
                }
            }
            // 如果处于多选状态，则根据选中状态设置背景颜色
            if (isMultiSelectMode) {
                if (selectedItems.contains(position)) {
                    view?.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.theme_color_gts))
                } else {
                    view?.setBackgroundResource(0)
                }
            }
            return view
        }

        private inner class ViewHolder(view: View) {
            val pageCollectIcon: ImageView = view.findViewById(R.id.collect_page_image_view)
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
        binding.includeTitleBarOperate.tvTitle.text = "已选" + selectedItems.size +"项"
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
                loadingDialog = ProgressDialog.show(this@InfoActivity,"数据加载中", "请稍后...", true, false)
                LoadDataTask().execute()
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
        filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.app_name) + File.separator + pkgName)
        if (filePath.exists() && filePath.isDirectory) {
            val files = filePath.listFiles()
            for (file in files) {
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
        filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.app_name))
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

    override fun onDestroy() {
        super.onDestroy()
        pageDataHelper.close()
    }

}