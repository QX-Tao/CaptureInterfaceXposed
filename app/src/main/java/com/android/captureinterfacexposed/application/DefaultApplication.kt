package com.android.captureinterfacexposed.application

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
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
                Toast.makeText(appContext,"应用已停止", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext,"应用停止失败", Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun startApp(packageName: String) {
            val cmd = "am start -n " + packageName + "/" + entryActivityClassName(packageName)
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, "应用已启动", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, "应用启动失败", Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun restartApp(packageName: String) {
            val cmd = "am start -S " + packageName + "/" + entryActivityClassName(packageName)
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, "应用已重启", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, "应用重启失败", Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun isDeviceRooted(): Boolean {
            val cmd = ""
            return ShellUtils.execCmd(cmd, true).result == 0
        }
        @JvmStatic
        fun allowPermission_SYSTEM_ALERT_WINDOW(packageName: String) {
            val cmd = "appops set $packageName SYSTEM_ALERT_WINDOW allow"
            ShellUtils.execCmd(cmd, true) // 指令有时会抽风，需要运行两次。
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, "已开启悬浮窗权限", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, "悬浮窗权限开启失败", Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun enableAccessibilityService(packageName: String, accessibilityServiceName: String) {
            val cmd =
                "settings put secure enabled_accessibility_services $packageName/$accessibilityServiceName && settings put secure accessibility_enabled 1"
            ShellUtils.execCmd(cmd, true) // 指令有时会抽风，需要运行两次。
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, "已开启应用无障碍设置", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, "应用无障碍设置开启失败", Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun allowPermission_PROJECT_MEDIA(packageName: String) {
            val cmd = "appops set $packageName PROJECT_MEDIA allow"
            ShellUtils.execCmd(cmd, true) // 指令有时会抽风，需要运行两次。
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, "已开启录屏权限", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, "录屏权限开启失败", Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun enableLSP(packageName: String) {
            val cmd = "/data/adb/lspd/bin/cli scope set -a $packageName android/0"
            ShellUtils.execCmd(cmd, true) // 指令有时会抽风，需要运行两次。
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, "已打开LSP作用域，请手动重启设备", Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun enableHookByLSP(packageName: String, hookAppPackage: String) {
            val cmd = "/data/adb/lspd/bin/cli scope set -s $packageName android/0 $hookAppPackage/0"
            ShellUtils.execCmd(cmd, true) // 指令有时会抽风，需要运行两次。
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, "Hook成功，正在打开应用", Toast.LENGTH_SHORT).show()
                restartApp(hookAppPackage)
            }
        }
        @JvmStatic
        fun getScreen(filePath: String) {
            val cmd = "screencap -p$filePath"
            if (ShellUtils.execCmd(cmd, true).result == 0) {
                Toast.makeText(appContext, "已截图", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(appContext, "截图失败", Toast.LENGTH_SHORT).show()
            }
        }
        @JvmStatic
        fun mkDir(path: String) {
            Thread {
                try {
                    val cmd = "mkdir -p $path"
                    if (ShellUtils.execCmd(cmd, true).result == 0) {
                        Toast.makeText(appContext, "创建文件夹成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(appContext, "文件夹已存在", Toast.LENGTH_SHORT).show()
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