package com.android.captureinterfacexposed.service

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager


/**
 * @author: QX_Tao
 * @date: 2023/5/4 19:27
 * @description: 监听器
 */
class ItemViewTouchListener(
    private val layoutParams: WindowManager.LayoutParams,
    private val windowManager: WindowManager
) : View.OnTouchListener {
    private var lastX = 0.0f
    private var lastY = 0.0f
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 这里接收不到Down事件，不要在这里写逻辑
            }
            MotionEvent.ACTION_MOVE -> {
                // 重写LinearLayout的OnInterceptTouchEvent之后，这里的Down事件会接收不到，所以初始位置需要在Move事件里赋值
                if (lastX == 0.0f || lastY == 0.0f) {
                    lastX = event.rawX
                    lastY = event.rawY
                }
                val nowX: Float = event.rawX
                val nowY: Float = event.rawY
                val movedX: Float = nowX - lastX
                val movedY: Float = nowY - lastY

                layoutParams.apply {
                    x += movedX.toInt()
                    y += movedY.toInt()
                }
                // 更新悬浮球控件位置
                windowManager.updateViewLayout(view, layoutParams)
                lastX = nowX
                lastY = nowY
            }
            MotionEvent.ACTION_UP -> {
                lastX = 0.0f
                lastY = 0.0f
            }
        }
        return true
    }
}