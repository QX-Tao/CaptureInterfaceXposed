package com.android.captureinterfacexposed.ui.activity

import android.view.View
import android.view.WindowManager
import com.android.captureinterfacexposed.databinding.ActivityPhotoViewBinding
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import com.blankj.utilcode.util.ImageUtils

class PhotoViewActivity : BaseActivity<ActivityPhotoViewBinding>() {
    override fun onCreate() {
        // 全屏，隐藏状态栏，显示导航栏
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)

        val screenPath = intent.getStringExtra("screenPath").toString()
        binding.pvPhotoview.setImageBitmap(ImageUtils.getBitmap(screenPath))
        binding.pvPhotoview.setOnClickListener{ onBackPressed() }

    }
}