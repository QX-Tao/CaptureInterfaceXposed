@file:Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package com.android.captureinterfacexposed.ui.activity.base

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.LayoutInflaterClass
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.utils.ShareUtil
import com.android.captureinterfacexposed.utils.factory.isNotSystemInDarkMode
import com.blankj.utilcode.util.LanguageUtils
import java.util.*

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private val THEME_MODE = "theme_key";
    private val LANGUAGE_MODE = "language_key";

    /**
     * Get the binding layout object
     *
     * 获取绑定布局对象
     */
    lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = current().generic()?.argument()?.method {
            name = "inflate"
            param(LayoutInflaterClass)
        }?.get()?.invoke<VB>(layoutInflater) ?: error("binding failed")
        setContentView(binding.root)
        /**
         * Hide Activity title bar
         * 隐藏系统的标题栏
         */
        supportActionBar?.hide()
        /**
         * Init immersive status bar
         * 初始化沉浸状态栏
         */
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isNotSystemInDarkMode
            isAppearanceLightNavigationBars = isNotSystemInDarkMode
        }
        ResourcesCompat.getColor(resources, R.color.colorThemeBackground, null).also {
            window?.statusBarColor = it
            window?.navigationBarColor = it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window?.navigationBarDividerColor = it
        }
        /**
         * Init children
         * 装载子类
         */
        onCreate()
    }

    /**
     * Callback [onCreate] method
     *
     * 回调 [onCreate] 方法
     */
    abstract fun onCreate()

}