package com.android.captureinterfacexposed.hook

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.android.captureinterfacexposed.BuildConfig
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.bridge.event.YukiXposedEvent
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.tencent.automationlib.Automation
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

@InjectYukiHookWithXposed(isUsingResourcesHook = false)
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        debugLog { tag = "CaptureInterface" }
        isDebug = BuildConfig.DEBUG
    }

    override fun onHook() = encase {
       // loadSystem(UIHierarchyHook)
    }

    override fun onXposedEvent() {
        super.onXposedEvent()
        YukiXposedEvent.events {
            onHandleLoadPackage {
                val lpparam = it
                if(!lpparam.isFirstApplication) return@onHandleLoadPackage
                val packageName = lpparam.packageName
                if ("android" == packageName || "com.android.captureinterfacexposed" == packageName){
                    return@onHandleLoadPackage
                } else {
                    XposedHelpers.findAndHookMethod(
                        Activity::class.java, "onCreate",
                        Bundle::class.java, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                               // val activity = param.thisObject as Activity
                               // Toast.makeText(activity, "Target app hook.", Toast.LENGTH_SHORT).show();
                               // Log.i("UIHierarchyHook", "onCreate called")
                                Automation.enable(1)
                                super.afterHookedMethod(param)
                            }
                        })
                }
            }
        }
    }
}