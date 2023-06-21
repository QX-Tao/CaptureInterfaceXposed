package com.android.captureinterfacexposed.hook

import android.app.Activity
import android.app.AndroidAppHelper
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.captureinterfacexposed.BuildConfig
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.bridge.event.YukiXposedEvent
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.tencent.automationlib.Automation
import com.tencent.automationlib.JsBridge
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
                            }
                        )
                    }
                    else -> {
                        // hook oncreate
                        XposedHelpers.findAndHookMethod(
                            Activity::class.java, "onCreate",
                            Bundle::class.java, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    Automation.enable(1)
                                    super.afterHookedMethod(param)
                                }
                            }
                        )

                        // hook onresume
                        XposedHelpers.findAndHookMethod(
                            Activity::class.java, "onResume", object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    val activity = param.thisObject as Activity
                                    val currentActivityName = activity.javaClass.name
                                    val intent = Intent("com.captureinterface.current_activity_name")
                                    intent.putExtra("activity_name", currentActivityName)
                                    XposedHelpers.callMethod(
                                        AndroidAppHelper.currentApplication(),
                                        "sendBroadcast",
                                        intent
                                    )
                                    Log.d("HookEntry", "Current activity: $currentActivityName")
                                }
                            }
                        )

                        // hook android webview
                        XposedHelpers.findAndHookConstructor(
                            WebView::class.java, Context::class.java, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    val webView = param.thisObject as WebView
                                    webView.settings.javaScriptEnabled = true
                                    webView.addJavascriptInterface(JsBridge.getInstance(),"JsBridge")
                                    WebView.setWebContentsDebuggingEnabled(true)
                                }
                            }
                        )
                        XposedHelpers.findAndHookConstructor(
                            WebView::class.java, Context::class.java,AttributeSet::class.java,
                            object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    val webView = param.thisObject as WebView
                                    webView.settings.javaScriptEnabled = true
                                    webView.addJavascriptInterface(JsBridge.getInstance(),"JsBridge")
                                    WebView.setWebContentsDebuggingEnabled(true)
                                }
                            }
                        )
                        XposedHelpers.findAndHookConstructor(
                            WebView::class.java, Context::class.java,AttributeSet::class.java,
                            Int::class.java, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    val webView = param.thisObject as WebView
                                    webView.settings.javaScriptEnabled = true
                                    webView.addJavascriptInterface(JsBridge.getInstance(),"JsBridge")
                                    WebView.setWebContentsDebuggingEnabled(true)
                                }
                            }
                        )
                        XposedHelpers.findAndHookConstructor(
                            WebView::class.java, Context::class.java, AttributeSet::class.java,
                            Int::class.java, Int::class.java, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    val webView = param.thisObject as WebView
                                    webView.settings.javaScriptEnabled = true
                                    webView.addJavascriptInterface(JsBridge.getInstance(),"JsBridge")
                                    WebView.setWebContentsDebuggingEnabled(true)
                                }
                            }
                        )
                        XposedHelpers.findAndHookMethod(
                            WebViewClient::class.java, "onPageFinished",
                            WebView::class.java, String::class.java, object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam?) {
                                    val webView = param!!.args[0] as WebView
                                    JsBridge.getInstance().injectJs(webView)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
