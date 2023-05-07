package com.android.captureinterfacexposed.ui.activity

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.android.captureinterfacexposed.BuildConfig
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.application.DefaultApplication
import com.android.captureinterfacexposed.databinding.ActivityMainBinding
import com.android.captureinterfacexposed.db.PageDataHelper
import com.android.captureinterfacexposed.service.CaptureInterfaceAccessibilityService
import com.android.captureinterfacexposed.service.FloatWindowService
import com.android.captureinterfacexposed.service.ScreenShotService
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import com.android.captureinterfacexposed.utils.ConfigUtil
import com.android.captureinterfacexposed.utils.CurrentCollectUtil
import com.highcapable.yukihookapi.YukiHookAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object{
        @JvmStatic
        private val LSP_HOOK = "lsp_hook"
        @JvmStatic
        private val WORK_MODE = "work_mode"
        @JvmStatic
        private val REQUEST_PERMISSIONS_CODE = 123
        @JvmStatic
        private val SCREENSHOT_REQUEST_CODE = 100
        @JvmStatic
        private val SELECT_APP_NAME = "select_app_name"
        @JvmStatic
        private val SELECT_PACKAGE_NAME = "select_package_name"
    }

    private var isServiceStart = false
    private var floatingView = FloatWindowService(this)
    private lateinit var loadingDialog: ProgressDialog
    private var isNeedProcessData = false

    private val startSelectAppActivityForResult = registerForActivityResult(SelectAppActivityResultContract()){ result ->
        result?.let {
            val (appName, packageName) = it
            binding.tvSelectAppTitle.text = "已选择: $appName"
            binding.tvSelectAppDesc.text = "包名: $packageName"
            ConfigUtil.getInstance(applicationContext).putString(SELECT_APP_NAME,appName)
            ConfigUtil.getInstance(applicationContext).putString(SELECT_PACKAGE_NAME,packageName)
            binding.llSelectAppMenu.visibility = View.VISIBLE
        }
    }

    override fun onCreate() {
        // TitleThemes
        binding.includeTitleBarFirst.tvTitle.text = getString(R.string.app_name)
        binding.includeTitleBarFirst.ivMoreButton.setOnClickListener { showPopupMenu(binding.includeTitleBarFirst.ivMoreButton) }

        // refresh status
        refreshModuleStatus()
        refreshSelectStatus()

        // isSelectWorkMode -> select a work mode
        if (!isSelectWorkMode()) { selectWorkModeDialog() }
        // running -> check permission
        else { checkRunningPermission() }

        if(isNeedProcessData) CoroutineScope(Dispatchers.IO).launch { processData() }

        // work_mode -> press to dialog
        binding.llWorkMode.setOnClickListener{workModeDialog()}

        // SharedPreferences listener
        val sharedPreferences : SharedPreferences =  PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if(key.equals(LSP_HOOK)){
                refreshSelectStatus()
                refreshModuleStatus()
            }
        }

        // select_app -> to SelectAppActivity, compile in method refreshModuleStatus()
        binding.tvOpenApp.setOnClickListener {
            val packageName = ConfigUtil.getInstance(applicationContext).getString(SELECT_PACKAGE_NAME,null)
            if (packageName != null){
                DefaultApplication.startApp(packageName)
            }
        }
        binding.tvClearSelect.setOnClickListener {
            ConfigUtil.getInstance(applicationContext).putString(SELECT_APP_NAME,null)
            ConfigUtil.getInstance(applicationContext).putString(SELECT_PACKAGE_NAME,null)
            refreshSelectStatus()
        }

        // check_data -> to DataActivity
        binding.llCheckData.setOnClickListener{
            val intent = Intent(this, DataActivity::class.java)
            startActivity(intent)
        }

        // to_github
        binding.llToGithub.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://github.com/QX-Tao/CaptureInterfaceXposed")
            startActivity(intent)
        }

        // to_help
        binding.llToHelp.setOnClickListener{helpDialog()}

        // to_setting -> to SettingsActivity
        binding.llToSetting.setOnClickListener{
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // button
        binding.btStartService.setOnClickListener {
            setUpFloatingView()
            binding.btStartService.visibility = View.GONE
            binding.btStopService.visibility = View.VISIBLE
            isServiceStart = true
        }
        binding.btStopService.setOnClickListener {
            stopFloatingView()
            binding.btStartService.visibility = View.VISIBLE
            binding.btStopService.visibility = View.GONE
            isServiceStart = false
        }
        binding.btStartCollect.setOnClickListener {
            val hookPackageName = ConfigUtil.getInstance(applicationContext).getString(SELECT_PACKAGE_NAME,null)
            if(isServiceStart){
                when(getWorkModeStatus()){
                    -1 ->{
                        CurrentCollectUtil.setLeftButtonClickable(true)
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.addCategory(Intent.CATEGORY_HOME)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    0 -> {
                        CurrentCollectUtil.setLeftButtonClickable(false)
                        Toast.makeText(applicationContext,"模块未激活，请重试。",Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        if (hookPackageName == null) {
                            Toast.makeText(applicationContext, "包名错误，请重试。", Toast.LENGTH_SHORT).show()
                        } else {
                            if(CurrentCollectUtil.getCollectPackageName() != null){
                                DefaultApplication.killApp(CurrentCollectUtil.getCollectPackageName())
                            }
                            CurrentCollectUtil.setCollectPackageName(hookPackageName)
                            CurrentCollectUtil.setRightButtonClickable(false)
                            CurrentCollectUtil.setLeftButtonClickable(true)
                            DefaultApplication.enableHookByLSP(packageName, hookPackageName)
                        }
                    }
                }
            } else {
                Toast.makeText(applicationContext,"服务未开启，请先开启服务。",Toast.LENGTH_SHORT).show()
            }
        }

    }

    /**
     * synchronize data between database and local
     *
     * 同步数据
     */
    private fun processData() {
        val mDbHelper = PageDataHelper(applicationContext)
        mDbHelper.clearDatabase()
        var filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        when(newDirectory(filePath.toString(), applicationContext.resources.getString(R.string.app_name))){
            true -> return
            false -> {
                filePath = File(filePath.toString() + File.separator + applicationContext.resources.getString(R.string.app_name))
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

    /**
     * Refresh module status
     *
     * 刷新模块状态
     */
    private fun refreshModuleStatus() {
        when(getWorkModeStatus()){
            -1 -> {
                binding.llWorkMode.setBackgroundResource(R.drawable.bg_yellow_solid)
                binding.ivWorkMode.setImageResource(R.drawable.ic_success_white)
                binding.tvWorkModeTitle.text = "普通模式"
                binding.tvWorkModeDesc.text = "使用普通模式运行中"
                ConfigUtil.getInstance(applicationContext).putString(SELECT_APP_NAME,null)
                ConfigUtil.getInstance(applicationContext).putString(SELECT_PACKAGE_NAME,null)
                refreshSelectStatus()
                binding.llSelectApp.setOnClickListener {
                    Toast.makeText(applicationContext,"普通模式下不需要选择应用",Toast.LENGTH_SHORT).show()
                }
                binding.btStartCollect.visibility = View.VISIBLE
            }
            0 -> {
                binding.llWorkMode.setBackgroundResource(R.drawable.bg_red_solid)
                binding.ivWorkMode.setImageResource(R.drawable.ic_failure_white)
                binding.tvWorkModeTitle.text = ("LSP注入模式(" + getString(R.string.module_not_activated) + ")")
                binding.tvWorkModeDesc.text = getString(R.string.module_version, BuildConfig.VERSION_NAME)
                binding.llSelectApp.setOnClickListener {
                    startSelectAppActivityForResult.launch(null)
                }
            }
            1 -> {
                binding.llWorkMode.setBackgroundResource(R.drawable.bg_green_solid)
                binding.ivWorkMode.setImageResource(R.drawable.ic_success_white)
                binding.tvWorkModeTitle.text = "LSP注入模式(" + getString(R.string.module_is_activated) + ")"
                binding.tvWorkModeDesc.text = getString(R.string.module_version, BuildConfig.VERSION_NAME)
                binding.llSelectApp.setOnClickListener {
                    startSelectAppActivityForResult.launch(null)
                }
            }
        }
    }

    /**
     * Refresh SelectApp status
     *
     * 刷新选择应用
     */
    private fun refreshSelectStatus(){
        val appName = ConfigUtil.getInstance(applicationContext).getString(SELECT_APP_NAME,null)
        val packageName = ConfigUtil.getInstance(applicationContext).getString(SELECT_PACKAGE_NAME,null)
        if (appName == null){
            binding.tvSelectAppTitle.text = getString(R.string.select_app)
        } else {
            binding.tvSelectAppTitle.text = "已选择: $appName"
        }
        if (packageName == null){
            binding.tvSelectAppDesc.text = getString(R.string.select_app_desc)
        } else {
            binding.tvSelectAppDesc.text = "包名: $packageName"
        }
        if (appName == null || packageName == null){
            binding.llSelectAppMenu.visibility = View.GONE
            binding.btStartCollect.visibility = View.GONE
        } else {
            binding.llSelectAppMenu.visibility = View.VISIBLE
            binding.btStartCollect.visibility = View.VISIBLE
        }
    }

    /**
     * Refresh all permission
     *
     * 刷新应用权限
     */
    private fun refreshPermissionStatus(): IntArray {
        val storagePermission = if(isStoragePermissionOn()) 1 else 0
        val allFilePermission = if(isAllFilePermissionOn()) 1 else 0
        val overlayPermission = if(isOverlayPermissionOn()) 1 else 0
        val accessPermission = if(isAccessibilitySettingsOn(applicationContext)) 1 else 0
        return intArrayOf(storagePermission,allFilePermission,overlayPermission,accessPermission)
    }

    /**
     * show popup menu
     *
     * 弹出菜单
     */
    private fun showPopupMenu(anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_popup, popupMenu.menu)
        popupMenu.menu.findItem(R.id.refresh_list).isVisible = false
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.restart_sys -> {DefaultApplication.reboot()}
                R.id.restart_app -> {DefaultApplication.restartApp(packageName)}
            }
            true
        }
        popupMenu.show()
    }

    /**
     * register for activity result to start SelectAppActivity
     *
     * 结果回调
     */
    class SelectAppActivityResultContract : ActivityResultContract<Void, Pair<String, String>?>() {
        override fun createIntent(context: Context, input: Void): Intent {
            return Intent(context, SelectAppActivity::class.java)
        }
        override fun parseResult(resultCode: Int, intent: Intent?): Pair<String, String>? {
            return when(resultCode){
                Activity.RESULT_OK -> intent?.let {
                    Pair(it.getStringExtra("app_name")?:"", it.getStringExtra("package_name")?:"")
                }
                else -> null
            }
        }
    }

    /**
     * on activity result to start ScreenShotService
     *
     * 结果回调
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 获取实时截图
        if (requestCode == SCREENSHOT_REQUEST_CODE && resultCode == RESULT_OK) {
            val intent = Intent(this, ScreenShotService::class.java)
            intent.putExtra("data", data)
            intent.putExtra("resultCode", resultCode)
            startService(intent) //启动服务
        }
    }

    /**
     * is select work mode -> is need to display dialog
     *
     * 是否已选择工作模式
     */
    private fun isSelectWorkMode(): Boolean {
        ConfigUtil.getInstance(applicationContext).getString(WORK_MODE,null)
            ?: return false
        return true
    }

    /**
     * select a work mode
     *
     * 选择工作模式
     */
    private fun selectWorkModeDialog() {
        isNeedProcessData = true
        val builder = AlertDialog.Builder(this@MainActivity)
        val alertDialog: AlertDialog = builder.setTitle("工作模式")
            .setMessage("在第一次打开应用时 需要选择工作模式")
            .setPositiveButton("普通模式"){ _, _ ->
                ConfigUtil.getInstance(applicationContext).putString(LSP_HOOK,false.toString())
                ConfigUtil.getInstance(applicationContext).putString(WORK_MODE,true.toString())
                refreshModuleStatus()
            }
            .setNegativeButton("LSP注入", null)
            .setCancelable(false)
            .show()
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(
            View.OnClickListener {
                if (DefaultApplication.isDeviceRooted()) {
                    ConfigUtil.getInstance(applicationContext).putString(LSP_HOOK,true.toString())
                    ConfigUtil.getInstance(applicationContext).putString(WORK_MODE,true.toString())
                    DefaultApplication.enableLSP(packageName)
                } else {
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).visibility = View.GONE
                    Toast.makeText(applicationContext, "请先授予应用ROOT权限", Toast.LENGTH_SHORT).show()
                    return@OnClickListener
                }
                refreshModuleStatus()
                alertDialog.cancel()
            })
        alertDialog.setOnDismissListener{
            checkRunningPermission()
        }
    }

    /**
     * work mode dialog to display info
     *
     * 运行信息
     */
    private fun workModeDialog() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("运行信息")
        when(getWorkModeStatus()){
            -1 -> builder.setMessage("当前使用普通模式，您需要手动对应用进行插桩及安装，点击开始收集后将返回桌面，进入插桩应用即可进行收集。")
            0 -> {
                DefaultApplication.enableLSP(packageName)
                builder.setMessage("已开启LSP注入模式，模块未激活，已尝试加载LSP作用域，可能需要重启手机。")
            }
            1 -> builder.setMessage("当前使用LSP注入模式，您需要选择一个应用，点击开始收集即可运行应用并进行收集。")
        }
        builder.setPositiveButton("确定",null)
        builder.create().show()
    }

    /**
     * help dialog to display message
     *
     * 提示信息
     */
    private fun helpDialog() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("提示")
        builder.setMessage("如LSP注入模式无法正常使用，可打开LSPosed查看状态，需确保模块作用域包含“系统框架(android)”和待测APP，点击“确定”以打开LSPosed")
        builder.setPositiveButton("确定") {_, _ -> DefaultApplication.startApp("org.lsposed.manager")}
        builder.create().show()
    }

    /**
     * set up floating view
     *
     * 开启浮窗 收集数据
     */
    private fun setUpFloatingView() {
        floatingView.setActivity(this)
        loadingDialog = ProgressDialog.show(this@MainActivity,"数据加载中", "请稍后...", true, false)
        LoadDataTask().execute()
    }

    /**
     * stop floating view
     *
     * 停止浮窗
     */
    private fun stopFloatingView() {
        floatingView.stopFloatingView()
    }

    /**
     * load data task
     *
     * 加载数据
     */
    private inner class LoadDataTask : AsyncTask<Void?, Void?, Void?>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): Void? {
            floatingView.processData()
            return null
        }
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(aVoid: Void?) {
            floatingView.startFloatingView()
            loadingDialog.dismiss() // 关闭进度条
        }
    }

    /**
     * check permission
     *
     * 检查权限
     */
    private fun checkRunningPermission() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.layout_permission_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("为了软件的正常运行，您需要授予以下权限才能使用应用。")
            .setView(dialogView)
            .setCancelable(false)
            .create()
        if(refreshPermissionStatus().contentEquals(intArrayOf(1, 1, 1, 1))) return
        else dialog.show()
        val storageLinearLayout = dialogView.findViewById<LinearLayout>(R.id.ll_storage_permission)
        val storageImageView = dialogView.findViewById<ImageView>(R.id.iv_storage_permission)
        val allFileLinearLayout = dialogView.findViewById<LinearLayout>(R.id.ll_all_file_permission)
        val allFileImageView = dialogView.findViewById<ImageView>(R.id.iv_all_file_permission)
        val overlayLinearLayout = dialogView.findViewById<LinearLayout>(R.id.ll_overlay_permission)
        val overlayImageView = dialogView.findViewById<ImageView>(R.id.iv_overlay_permission)
        val accessLinearLayout = dialogView.findViewById<LinearLayout>(R.id.ll_access_permission)
        val accessImageView = dialogView.findViewById<ImageView>(R.id.iv_access_permission)
        val confirmButton = dialogView.findViewById<TextView>(R.id.bt_permission_confirm)
        val refreshButton = dialogView.findViewById<TextView>(R.id.bt_permission_refresh)
        val checkDataButton = dialogView.findViewById<TextView>(R.id.bt_permission_check_data)
        storageImageView.setImageResource(
            when{
                isStoragePermissionOn() -> R.drawable.ic_check_permission
                else -> R.drawable.ic_check_permission_no
            }
        )
        allFileImageView.setImageResource(
            when{
                isAllFilePermissionOn() -> R.drawable.ic_check_permission
                else -> R.drawable.ic_check_permission_no
            }
        )
        overlayImageView.setImageResource(
            when{
                isOverlayPermissionOn() -> R.drawable.ic_check_permission
                else -> R.drawable.ic_check_permission_no
            }
        )
        accessImageView.setImageResource(
            when{
                isAccessibilitySettingsOn(applicationContext) -> R.drawable.ic_check_permission
                else -> R.drawable.ic_check_permission_no
            }
        )
        allFileLinearLayout.visibility = (
                when(Build.VERSION.SDK_INT < Build.VERSION_CODES.R ){
                    true -> View.GONE
                    false -> View.VISIBLE
                })
        storageLinearLayout.setOnClickListener{ checkStoragePermission() }
        allFileLinearLayout.setOnClickListener { checkAllFilePermission() }
        overlayLinearLayout.setOnClickListener { applyOverlayPermission() }
        accessLinearLayout.setOnClickListener { checkAccessibility() }
        /**
         * can't enter without all permission allow
         *
         * 没授予所有权限时不能进入应用
         */
        confirmButton.setOnClickListener {
            if (refreshPermissionStatus().contentEquals(intArrayOf(1, 1, 1, 1))) {
                dialog.cancel()
            } else{
                Toast.makeText(applicationContext,"请授予所有权限，点击刷新可刷新权限状态",Toast.LENGTH_SHORT).show()
            }
        }
        refreshButton.setOnClickListener {
            val permissionStatus = refreshPermissionStatus()
            storageImageView.setImageResource(
                when{
                    permissionStatus[0] == 1 -> R.drawable.ic_check_permission
                    else -> R.drawable.ic_check_permission_no
                }
            )
            allFileImageView.setImageResource(
                when{
                    permissionStatus[1] == 1 -> R.drawable.ic_check_permission
                    else -> R.drawable.ic_check_permission_no
                }
            )
            overlayImageView.setImageResource(
                when{
                    permissionStatus[2] == 1 -> R.drawable.ic_check_permission
                    else -> R.drawable.ic_check_permission_no
                }
            )
            accessImageView.setImageResource(
                when{
                    permissionStatus[3] == 1 -> R.drawable.ic_check_permission
                    else -> R.drawable.ic_check_permission_no
                }
            )
        }
        checkDataButton.setOnClickListener {
            val permissionStatus = refreshPermissionStatus()
            if(permissionStatus[0] == 1 && permissionStatus[1] == 1) {
                CoroutineScope(Dispatchers.IO).launch { processData() }
                val intent = Intent(this, DataActivity::class.java)
                startActivity(intent)
                isNeedProcessData = false
            } else {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    Toast.makeText(applicationContext, "查看结果需要存储权限", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "查看结果需要存储权限和所有文件权限", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * on request permission result
     *
     * 权限回调
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // 未授予读写权限，动态请求权限
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(applicationContext, "请授予应用存储权限。", Toast.LENGTH_SHORT).show()
                    // 用户拒绝授权，但未选择 "不再询问"，延迟一秒后再次申请
                    Handler(Looper.getMainLooper()).postDelayed({
                        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSIONS_CODE) }, 1000)
                } else {
                    // 用户选择了 "不再询问"，引导用户前往应用设置页面手动授权
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                    Toast.makeText(applicationContext, "请授予应用存储权限。", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * check storage permission
     *
     * 检查存储权限
     */
    private fun checkStoragePermission(){
        if (!isStoragePermissionOn()) {
            // 第一次申请
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSIONS_CODE)
        }
    }

    /**
     * is storage permission on
     *
     * 检查存储权限
     */
    private fun isStoragePermissionOn() : Boolean{
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    /**
     * check all file permission
     *
     * 检查所有文件权限
     */
    private fun checkAllFilePermission(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (!isAllFilePermissionOn()) {
            Toast.makeText(applicationContext, "请找到“界面信息收集”，并打开所有文件权限。", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        }
    }

    /**
     * is all file permission on
     *
     * 检查所有文件权限
     */
    private fun isAllFilePermissionOn(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()) {
            return true
        }
        return false
    }


    /**
     * check draw overlay permission
     *
     * 检查截图权限
     */
    private fun applyOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(applicationContext, "请找到“界面信息收集”，并打开悬浮窗权限。", Toast.LENGTH_SHORT).show()
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    /**
     * is draw overlay permission on
     *
     * 检查截图权限
     */
    private fun isOverlayPermissionOn() : Boolean {
        if (Settings.canDrawOverlays(this)) {
            return true
        }
        return false
    }

    /**
     * check accessibility service
     *
     * 检查无障碍服务
     */
    private fun checkAccessibility() {
        if (!isAccessibilitySettingsOn(this)) {
            Toast.makeText(applicationContext, "请打开“界面信息收集”的无障碍功能", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    /**
     * is accessibility service on
     *
     * 检查无障碍服务
     */
    private fun isAccessibilitySettingsOn(mContext: Context): Boolean {
        var accessibilityEnabled = 0
        val service = packageName + "/" + CaptureInterfaceAccessibilityService::class.java.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                mContext.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: SettingNotFoundException) {
            Log.e(
                ContentValues.TAG,
                "Error finding setting, default accessibility to not found: " + e.message
            )
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                mContext.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * check work mode: normal -> -1, lsp_off -> 0, lsp_on -> 1
     *
     * 返回运行模式：普通、LSP（未激活）、LSP（已激活）
     */
    private fun getWorkModeStatus() : Int {
        val isLspHook = ConfigUtil.getInstance(applicationContext).getString(LSP_HOOK,null)
        return if(isLspHook == true.toString()) {
            if(YukiHookAPI.Status.isXposedModuleActive){
                1
            } else {
                0
            }
        } else {
            -1
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSelectStatus()
        refreshModuleStatus()
    }
}