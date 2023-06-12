package com.android.captureinterfacexposed.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.IBinder
import android.text.TextUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.application.DefaultApplication.Companion.entryActivityClassName
import com.android.captureinterfacexposed.databinding.ActivitySelectAppBinding
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SelectAppActivity : BaseActivity<ActivitySelectAppBinding>() {
    companion object {
        private var appList: MutableList<ApplicationInfo>? = null
        private var appItemList: ArrayList<AppItem>? = null
    }
    private val posMap = HashMap<Int, Int>()
    private var adapter: AppListAdapter? = null
    private var isProcessBarStatus = false

    override fun onCreate() {
        binding.includeTitleBarSecond.tvTitle.text = getText(R.string.select_app)
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener { finish() }
        binding.includeTitleBarSecond.ivMoreButton.setOnClickListener { showPopupMenu(binding.includeTitleBarSecond.ivMoreButton) }
        if (appList == null || appItemList == null) {
            lifecycleScope.launch { loadData() }
        } else {
            adapter = AppListAdapter(appItemList!!)
            binding.appListView.adapter = adapter
        }
        binding.searchView.isSubmitButtonEnabled = false
        binding.searchView.onActionViewExpanded()
        binding.searchView.clearFocus()

        // 添加搜索框的监听器
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if(isProcessBarStatus) return true
                if (TextUtils.isEmpty(newText)) {
                    // 没有输入的时候就显示所有
                    setAdapter()
                } else {
                    val list: MutableList<AppItem> = ArrayList()
                    for (app in appList!!) {
                        val appName = app.loadLabel(packageManager).toString()
                        val packageName = app.packageName
                        val versionName = getVersionName(packageName)
                        val size = getSize(app)
                        val appIcon = app.loadIcon(packageManager)
                        val item = "$appName ($packageName)"
                        if (item.contains(newText)) {
                            val index = appList!!.indexOf(app)
                            val appItem = AppItem(item, appName, versionName, size, appIcon, index)
                            list.add(appItem)
                        }
                    }
                    val newAppItemList = ArrayList(list)
                    adapter = AppListAdapter(newAppItemList)
                    binding.appListView.adapter = adapter
                }
                return true
            }
        })
        binding.appListView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val appName = appList!![posMap[position]!!].loadLabel(this@SelectAppActivity.packageManager).toString()
                val packageName = appList!![posMap[position]!!].packageName
                // 将选择的应用名称和包名返回给 MainActivity
                val intent = Intent()
                intent.putExtra("app_name", appName)
                intent.putExtra("package_name", packageName)
                setResult(RESULT_OK, intent)
                finish()
            }
        onBackPressedDispatcher.addCallback(
            this, // lifecycle owner
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!isProcessBarStatus) { finish() }
                }
            })
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun processData() {
        // 获取本机已安装应用信息
        val appList1 = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        appList = ArrayList()
        for (app in appList1) {
            // 筛选有活动入口的应用程序
            if (entryActivityClassName(app.packageName) != null && app.packageName != applicationInfo.packageName) {
                (appList as ArrayList<ApplicationInfo>).add(app)
            }
        }
    }

    private fun setAdapter() {
        appItemList = ArrayList()
        for (app in appList!!) {
            val appName = app.loadLabel(packageManager).toString()
            val packageName = app.packageName
            val versionName = getVersionName(packageName)
            val size = getSize(app)
            val appIcon = app.loadIcon(packageManager)
            val index = appList!!.indexOf(app)
            val appItem = AppItem(
                "$appName ($packageName)", appName, versionName, size, appIcon, index
            )
            appItemList!!.add(appItem)
        }
        adapter = AppListAdapter(appItemList!!)
        binding.appListView.adapter = adapter
    }

    private inner class AppListAdapter(private val appItemList: ArrayList<AppItem>) :
        BaseAdapter() {
        override fun getCount(): Int {
            return appItemList.size
        }

        override fun getItem(position: Int): String {
            return appItemList[position].toString()
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var view = convertView
            val holder: ViewHolder
            if (view == null) {
                view = LayoutInflater.from(applicationContext).inflate(R.layout.app_list_item, parent, false)
                holder = ViewHolder(view)
                view.tag = holder
            } else {
                holder = view.tag as ViewHolder
            }
            val index = appItemList[position].index
            val appIcon = appItemList[position].appIcon
            val appNames = appItemList[position].appNames
            val versionName = appItemList[position].appVersion
            val size = appItemList[position].appSize
            holder.appIconImageView.setImageDrawable(appIcon)
            holder.appNamesTextView.text = appNames
            holder.appVersionTextView.text = resources.getString(R.string.app_version, versionName)
            holder.appSizeTextView.text = size
            holder.appNamesTextView.setTextColor(resources.getColor(R.color.firstTextColor))
            holder.appVersionTextView.setTextColor(resources.getColor(R.color.secondTextColor))
            holder.appSizeTextView.setTextColor(resources.getColor(R.color.secondTextColor))
            posMap[position] = index
            return view
        }

        private inner class ViewHolder(view: View) {
            val appIconImageView: ImageView = view.findViewById(R.id.app_icon_image_view)
            val appNamesTextView:TextView = view.findViewById(R.id.app_name_text_view)
            val appVersionTextView:TextView = view.findViewById(R.id.app_version_text_view)
            val appSizeTextView:TextView = view.findViewById(R.id.app_size_text_view)
        }
    }

    private fun getVersionName(packageName: String): String {
        val packageManager = packageManager
        var versionName = ""
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionName = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionName
    }

    private fun getSize(app: ApplicationInfo): String {
        val apkPath = app.sourceDir
        val size = File(apkPath).length()
        return Formatter.formatFileSize(this, size)
    }

    private class AppItem(
        val appNames: String,
        val appName: String,
        val appVersion: String,
        val appSize: String,
        val appIcon: Drawable,
        val index: Int
    ) {
        override fun toString(): String {
            return """
                appNames: $appNames
                appName: $appName
                appVersion: $appVersion
                appSize$appSize
                """.trimIndent()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (isHideInput(view, ev)) {
                hideSoftInput(view!!.windowToken)
                view.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    // 判定是否需要隐藏
    private fun isHideInput(v: View?, ev: MotionEvent): Boolean {
        if (v is EditText) {
            val l = intArrayOf(0, 0)
            v.getLocationInWindow(l)
            val left = l[0]
            val top = l[1]
            val bottom = top + v.getHeight()
            val right = left + v.getWidth()
            return ev.x <= left || ev.x >= right || ev.y <= top || ev.y >= bottom
        }
        return false
    }

    // 隐藏软键盘
    private fun hideSoftInput(token: IBinder?) {
        if (token != null) {
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_popup, popupMenu.menu)
        popupMenu.menu.findItem(R.id.restart_sys).isVisible = false
        popupMenu.menu.findItem(R.id.restart_app).isVisible = false
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == R.id.refresh_list) {
                binding.searchView.setQuery("", false)
                binding.appListView.adapter = null
                appList!!.clear()
                appItemList!!.clear()
                lifecycleScope.launch { loadData() }
            }
            true
        }
        popupMenu.show()
    }

    private suspend fun loadData() {
        inProcessBar()
        withContext(Dispatchers.IO) {
            processData()
        }
        outProcessBar()
        withContext(Dispatchers.Main) {
            setAdapter() // 刷新列表
        }
    }

    private fun inProcessBar(){
        isProcessBarStatus = true
        binding.progressBar.visibility = View.VISIBLE
        binding.includeTitleBarSecond.ivBackButton.isEnabled = false
        binding.includeTitleBarSecond.ivMoreButton.isEnabled = false
    }

    private fun outProcessBar(){
        isProcessBarStatus = false
        binding.progressBar.visibility = View.GONE
        binding.includeTitleBarSecond.ivBackButton.isEnabled = true
        binding.includeTitleBarSecond.ivMoreButton.isEnabled = true
    }

}