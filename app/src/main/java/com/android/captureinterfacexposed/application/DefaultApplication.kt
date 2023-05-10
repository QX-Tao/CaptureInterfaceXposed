package com.android.captureinterfacexposed.application

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.utils.ShareUtil
import com.blankj.utilcode.util.LanguageUtils
import com.blankj.utilcode.util.ShellUtils
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import java.util.*

class DefaultApplication : ModuleApplication() {

    private val THEME_MODE = "theme_key";
    private val LANGUAGE_MODE = "language_key";

    override fun onCreate() {
        super.onCreate()

        /**
         * theme mode setting
         * 主题模式
         */
        when (ShareUtil.getString(applicationContext,THEME_MODE,"follow_system")) {
            "follow_system" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            "light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        /**
         * language mode setting
         * 语言模式
         */
        when (ShareUtil.getString(applicationContext,LANGUAGE_MODE,"follow_system")) {
            "follow_system" -> {
                LanguageUtils.applySystemLanguage(false)
            }
            "zh_CN" -> {
                LanguageUtils.applyLanguage(Locale.SIMPLIFIED_CHINESE,false)
            }
            "zh_TW" -> {
                LanguageUtils.applyLanguage(Locale.TRADITIONAL_CHINESE,false)
            }
            "english" -> {
                LanguageUtils.applyLanguage(Locale.US,false)
            }
        }
    }



    companion object{
        @JvmStatic
        fun entryActivityClassName(str: String): String? {
            val intent = Intent("android.intent.action.MAIN", null as Uri?)
            intent.addCategory("android.intent.category.LAUNCHER")
            val queryIntentActivities = appContext.packageManager?.queryIntentActivities(intent, 0)
            if (queryIntentActivities != null) {
                for (i in queryIntentActivities.indices) {
                    val activityInfo = queryIntentActivities[i].activityInfo
                    val str2 = activityInfo.name
                    if (activityInfo.packageName == str) {
                        return str2
                    }
                }
            }
            return null
        }
        @JvmStatic
        fun killApp(packageName: String) {
            val cmd = "am force-stop $packageName"
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.success_stop_app), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, appContext.resources.getString(R.string.failure_stop_app), Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun startApp(packageName: String) {
            val cmd = "am start -n " + packageName + "/" + entryActivityClassName(packageName)
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.success_start_app), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, appContext.resources.getString(R.string.failure_start_app), Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun restartApp(packageName: String) {
            val cmd = "am start -S " + packageName + "/" + entryActivityClassName(packageName)
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.success_restart_app), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, appContext.resources.getString(R.string.failure_restart_app), Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun isDeviceRooted(): Boolean {
            val cmd = ""
            return ShellUtils.execCmd(cmd, true).result == 0
        }
        @JvmStatic
        fun allowPermissionSYSTEMALERTWINDOW(packageName: String) {
            val cmd = "appops set $packageName SYSTEM_ALERT_WINDOW allow"
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.success_allow_SYSTEM_ALERT_WINDOW_permission), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, appContext.resources.getString(R.string.failure_allow_SYSTEM_ALERT_WINDOW_permission), Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun enableAccessibilityService(packageName: String, accessibilityServiceName: String) {
            val cmd = "settings put secure enabled_accessibility_services $packageName/$accessibilityServiceName && settings put secure accessibility_enabled 1"
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.success_allow_accessibility_service), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, appContext.resources.getString(R.string.failure_allow_accessibility_service), Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun allowPermissionPROJECTMEDIA(packageName: String) {
            val cmd = "appops set $packageName PROJECT_MEDIA allow"
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.success_allow_PROJECT_MEDIA_permission), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, appContext.resources.getString(R.string.failure_allow_PROJECT_MEDIA_permission), Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun enableLSP(packageName: String) {
            val cmd = "/data/adb/lspd/bin/cli scope set -a $packageName android/0"
            ShellUtils.execCmd(cmd, true) // 指令有时会抽风，需要运行两次。
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.enable_module_LSP_scope), Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun enableHookByLSP(packageName: String, hookAppPackage: String) {
            val cmd = "/data/adb/lspd/bin/cli scope set -s $packageName android/0 $hookAppPackage/0"
            ShellUtils.execCmd(cmd, true) // 指令有时会抽风，需要运行两次。
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.enable_hook_by_LSP), Toast.LENGTH_SHORT).show()
                restartApp(hookAppPackage)
            }
        }
        @JvmStatic
        fun getScreen(filePath: String) {
            val cmd = "screencap -p$filePath"
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, appContext.resources.getString(R.string.success_screen), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, appContext.resources.getString(R.string.failure_screen), Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun mkDir(path: String) {
            Thread {
                try {
                    val cmd = "mkdir -p $path"
                    if (ShellUtils.execCmd(cmd, true).result == 0) {
                        Toast.makeText(appContext, appContext.resources.getString(R.string.success_mkdir), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(appContext, appContext.resources.getString(R.string.failure_mkdir), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
        @JvmStatic
        fun reboot() {
            val cmd = "reboot"
            ShellUtils.execCmd(cmd, true)
        }
    }

}