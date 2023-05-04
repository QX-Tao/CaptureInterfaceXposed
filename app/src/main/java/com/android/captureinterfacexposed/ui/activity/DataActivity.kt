package com.android.captureinterfacexposed.ui.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.android.captureinterfacexposed.databinding.ActivityDataBinding
import com.android.captureinterfacexposed.db.PageDataHelper
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity

class DataActivity : BaseActivity<ActivityDataBinding>() {

    companion object{
        private lateinit var pageDataHelper: PageDataHelper
        private lateinit var loadingDialog: ProgressDialog
        private var pageItemList: List<PageItem>? = null
        private var pageTmpList:  List<PageDataHelper.Page>? = null
    }

    override fun onCreate() {

        binding.includeTitleBarSecond.tvTitle.text = getString(R.string.cellect_result)
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener { onBackPressed() }
        binding.includeTitleBarSecond.ivMoreButton.setOnClickListener { showPopupMenu(binding.includeTitleBarSecond.ivMoreButton) }

        pageDataHelper = PageDataHelper(this)

        loadingDialog = ProgressDialog.show(this@DataActivity,"数据加载中", "请稍后...", true, false)
        LoadDataTask().execute()

        binding.pageItemListView.setOnItemClickListener { _, _, position, _ ->
            val appName = pageItemList?.get(position)?.appName
            val pkgName = pageItemList?.get(position)?.packageName
            val mid = pageItemList?.get(position)?.mid
            val intent = Intent(this,InfoActivity::class.java)
            intent.putExtra("app_name", appName)
            intent.putExtra("package_name", pkgName)
            intent.putExtra("mid",mid)
            startActivity(intent)
        }

    }

    private inner class LoadDataTask : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            processData()
            return null
        }
        override fun onPostExecute(aVoid: Void?) {
            val pageItemAdapter = PageItemListAdapter(applicationContext, pageItemList!!)
            binding.pageItemListView.adapter = pageItemAdapter
            loadingDialog.dismiss() // 关闭进度条
        }
    }

    fun processData(){
        pageTmpList = pageDataHelper.allPages
        pageItemList = getPageItemList(pageTmpList!!)
    }

    /**
     * page item list adapter
     *
     * 列表适配器
     */
    private class PageItemListAdapter(private val context: Context, private val pageItemList: List<PageItem>): BaseAdapter() {
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
                view = LayoutInflater.from(context).inflate(R.layout.data_list_item,parent,false)
                holder = ViewHolder(view)
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
            holder.pageNum.text = index.pageNum + "份"
            return view
        }

        private class ViewHolder(view: View) {
            val appIcon: ImageView = view.findViewById(R.id.app_icon_image_view)
            val appName: TextView = view.findViewById(R.id.app_name_text_view)
            val packageName: TextView = view.findViewById(R.id.package_name_text_view)
            val pageNum: TextView = view.findViewById(R.id.page_num_text_view)
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
                pageItemList = null
                pageTmpList = null
                binding.pageItemListView.adapter = null
                loadingDialog = ProgressDialog.show(this@DataActivity,"数据加载中", "请稍后...", true, false)
                LoadDataTask().execute()
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
     * get app name by its packageName
     *
     * 获取应用图标
     */
    private fun getAppNameByPkgName(packageName:String): String? {
        val packageManager = applicationContext.packageManager
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(appInfo).toString()
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


    override fun onDestroy() {
        super.onDestroy()
        pageDataHelper.close()
    }

}