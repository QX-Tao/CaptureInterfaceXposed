package com.android.captureinterfacexposed.hook

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
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
import java.io.File

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
                when (lpparam.packageName) {
                    "android" -> {
                        return@onHandleLoadPackage
                    }
                    "com.android.captureinterfacexposed" -> {
                        XposedHelpers.findAndHookMethod(
                            "android.app.ContextImpl",lpparam.classLoader, "getSharedPreferencesPath",
                            String::class.java, object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam?) {
                                    val name: String = param!!.args[0] as String
                                    val preferencesDir = File("/data/data/com.android.captureinterfacexposed/shared_prefs")
                                    Log.d("ContextImplHook", "getSharedPreferencesPath: $name")
                                    param.result = File("$preferencesDir/$name.xml")
                                }
                            })
                    }
                    else -> {
                        XposedHelpers.findAndHookMethod(
                            Activity::class.java, "onCreate",
                            Bundle::class.java, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    Automation.enable(1)
                                    super.afterHookedMethod(param)
                                }
                            })
                        XposedHelpers.findAndHookMethod(
                            "android.webkit.WebView", lpparam.classLoader, "<init>",
                            String::class.java, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    val name: String = param.args[0] as String
                                    super.afterHookedMethod(param)
                                }
                            })
                    }
                }
            }
        }
    }
}
