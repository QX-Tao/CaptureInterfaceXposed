package com.android.captureinterfacexposed.ui.activity

import android.app.ProgressDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.databinding.ActivityInfoBinding
import com.android.captureinterfacexposed.db.PageDataHelper
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import kotlin.properties.Delegates

class InfoActivity : BaseActivity<ActivityInfoBinding>(){

    companion object{
        private lateinit var pageDataHelper: PageDataHelper
        private lateinit var loadingDialog: ProgressDialog
        private var mid by Delegates.notNull<Long>()
        private var pageCollectItemList: List<PageCollectItem>? = null
        private var pageTmpCollectList: List<PageDataHelper.PageCollect>? = null
    }

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
    }

    private inner class LoadDataTask : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            processData()
            return null
        }
        override fun onPostExecute(aVoid: Void?) {
            val pageCollectItemAdapter = PageCollectItemListAdapter(applicationContext, pageCollectItemList!!)
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
    private class PageCollectItemListAdapter(private val context: Context, private val pageCollectItemList: List<PageCollectItem>): BaseAdapter() {
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
                view = LayoutInflater.from(context).inflate(R.layout.info_list_item,parent,false)
                holder = ViewHolder(view)
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
            return view
        }

        private class ViewHolder(view: View) {
            val pageCollectIcon: ImageView = view.findViewById(R.id.collect_page_image_view)
            val pageCollectData: TextView = view.findViewById(R.id.page_collect_data_text_view)
            val pageCollectNum: TextView = view.findViewById(R.id.page_collect_num_text_view)
        }

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