package com.android.captureinterfacexposed.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

/**
 * @author: QX_Tao
 * @date: 2023/5/4 18:49
 * @description: 可拖拽的LinearLayout，解决子View设置OnClickListener之后无法拖拽的问题
 */
class DraggableLinearLayout : LinearLayout {
    constructor(context: Context, attr: AttributeSet) : this(context, attr, 0)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : this(context, attr, defStyleAttr, 0)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attr, defStyleAttr, defStyleRes)
 
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_MOVE -> {
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}