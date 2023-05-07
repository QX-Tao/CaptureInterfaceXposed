package com.android.captureinterfacexposed.ui.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.application.DefaultApplication
import com.android.captureinterfacexposed.databinding.ActivitySettingsBinding
import com.android.captureinterfacexposed.db.PageDataHelper
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import com.android.captureinterfacexposed.utils.ConfigUtil
import java.io.File

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    companion object{
        @JvmStatic
        private val LSP_HOOK = "lsp_hook";
        private lateinit var loadingDialog: ProgressDialog
    }

    override fun onCreate() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()

        binding.includeTitleBarSecond.tvTitle.text = getString(R.string.settings)
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener { onBackPressed() }
        binding.includeTitleBarSecond.ivMoreButton.visibility = View.INVISIBLE

    }

    class SettingsFragment() : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val isLspHook = ConfigUtil.getInstance(context).getString(LSP_HOOK,null)
            val switchPreference = findPreference<SwitchPreferenceCompat>(LSP_HOOK)
            switchPreference?.isChecked = isLspHook == true.toString()
            switchPreference!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener setOnPreferenceChangeListener@{ _: Preference?, newValue: Any ->
                    val isChecked = newValue as Boolean
                    if (isChecked) {
                        if (!DefaultApplication.isDeviceRooted()) {
                            Toast.makeText(context, "请先授予应用ROOT权限", Toast.LENGTH_SHORT)
                                .show()
                            return@setOnPreferenceChangeListener false
                        }
                    }
                    ConfigUtil.getInstance(context).putString(LSP_HOOK,isChecked.toString())
                    true
                }

            val syncButton = findPreference<Preference>("sync_data")
            syncButton!!.setOnPreferenceClickListener {
                loadingDialog = ProgressDialog.show(context,"数据加载中", "请稍后...", true, false)
                    LoadDataTask().execute()
                true
            }
        }

        private inner class LoadDataTask() : AsyncTask<Void?, Void?, Void?>() {
            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg params: Void?): Void? {
                processData()
                return null
            }
            @Deprecated("Deprecated in Java")
            override fun onPostExecute(aVoid: Void?) {
                loadingDialog.dismiss() // 关闭进度条
                Toast.makeText(context,"数据已加载",Toast.LENGTH_SHORT).show()
            }

            /**
             * synchronize data between database and local
             *
             * 同步数据
             */
            private fun processData() {
                val mDbHelper = PageDataHelper(context!!.applicationContext)
                mDbHelper.clearDatabase()
                var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                when(newDirectory(filePath.toString(), context!!.applicationContext.resources.getString(R.string.app_name))){
                    true -> return
                    false -> {
                        filePath = File(filePath.toString() + File.separator + context!!.applicationContext.resources.getString(R.string.app_name))
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
                val packageManager: PackageManager = context!!.applicationContext.packageManager
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    return packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
                return null
            }
        }
    }
}