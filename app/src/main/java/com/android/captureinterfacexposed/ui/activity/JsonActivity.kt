package com.android.captureinterfacexposed.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.databinding.ActivityJsonBinding
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.demo.jsonpreviewer.EditJsonViewModel
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock

class JsonActivity : BaseActivity<ActivityJsonBinding>() {

    private lateinit var viewModel: EditJsonViewModel
    private lateinit var dispatcher: OnBackPressedDispatcher
    private lateinit var callback: OnBackPressedCallback
    private lateinit var jsonFilePath: String
    private val jsonLock = ReentrantLock()
    private var contentChanged: Boolean = false
    private var isChanged: Boolean = false

    override fun onCreate() {

        jsonFilePath = intent.getStringExtra("jsonFilePath").toString()

        binding.includeTitleBarSave.tvTitle.text = getText(R.string.edit_json)
        binding.includeTitleBarSave.ivBackButton.setOnClickListener { dispatcher.onBackPressed() }
        binding.includeTitleBarSave.ivSaveButton.setOnClickListener {
            binding.webView.evaluateJavascript("javascript:getJson()") {
                when {
                    it.equals("null") -> {}
                    else -> {
                        isChanged = true
                        var decodeJson = StringEscapeUtils.unescapeJson(it)
                        decodeJson = decodeJson.substring(1,decodeJson.length - 1)
                        Toast.makeText(applicationContext, decodeJson, Toast.LENGTH_SHORT).show()
                        saveFile(decodeJson)
                        contentChanged = false
                    }
                }
            }
        }

        val jsonStr =  FileIOUtils.readFile2String(jsonFilePath)

        dispatcher = onBackPressedDispatcher
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(contentChanged){
                    val builder = AlertDialog.Builder(this@JsonActivity)
                    builder.setTitle(getString(R.string.data_changed)).setMessage(getString(R.string.data_changed_desc))
                        .setPositiveButton(getString(R.string.save_and_exit)) { _, _ ->
                            binding.webView.evaluateJavascript("javascript:getJson()") {
                                when {
                                    it.equals("null") -> {}
                                    else -> {
                                        isChanged = true
                                        var decodeJson = StringEscapeUtils.unescapeJson(it)
                                        decodeJson = decodeJson.substring(1,decodeJson.length - 1)
                                        saveFile(decodeJson)
                                        val resultIntent = Intent()
                                        resultIntent.putExtra("isChanged", isChanged)
                                        setResult(RESULT_OK, resultIntent)
                                        finish()
                                    }
                                }
                            }
                        }
                        .setNegativeButton(getString(R.string.do_not_save)) { _, _ ->
                            val resultIntent = Intent()
                            resultIntent.putExtra("isChanged", isChanged)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                        .setCancelable(true)
                        .create()
                        .show()
                } else {
                    val resultIntent = Intent()
                    resultIntent.putExtra("isChanged", isChanged)
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
        dispatcher.addCallback(callback)

        viewModel = ViewModelProvider(
            viewModelStore,
            ViewModelProvider.AndroidViewModelFactory(this.application)
        )[EditJsonViewModel::class.java]
        initWebView()
        viewModel.jsonData.observe(this) {
            binding.webView.loadUrl("javascript:showJson($jsonStr)")
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                viewModel.loadConfig(this@JsonActivity)
            }
        }
        binding.webView.loadUrl("file:///android_asset/preview_json.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportZoom(true)
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        binding.webView.addJavascriptInterface(JsInterface(this@JsonActivity), "json_parse")
    }

    inner class JsInterface(context: Context) {
        private val mContext: Context

        init {
            mContext = context
        }

        @JavascriptInterface
        fun configContentChanged() {
            runOnUiThread {
                contentChanged = true
            }
        }

        @JavascriptInterface
        fun toastJson(msg: String?) {
            runOnUiThread { Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show() }
        }

        @JavascriptInterface
        fun parseJsonException(e: String?) {
            runOnUiThread {
                e?.takeIf { it.isNotBlank() }?.let { alert(it) }
            }
        }
    }

    private fun alert(
        message: String,
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.error)).setMessage(message)
            .setPositiveButton(getString(R.string.confirm), null)
            .setCancelable(false)
            .create()
            .show()
    }

    private fun saveFile(jsonStr: String){
        val jsonFile = File(jsonFilePath)
        if (FileUtils.isFileExists("$jsonFilePath.bak"))
            FileUtils.delete("$jsonFilePath.bak")
        FileUtils.rename(jsonFilePath, jsonFile.name + ".bak")
        val fileOutputStream = FileOutputStream(jsonFile, true)
        jsonLock.lock()
        try {
            fileOutputStream.use {
                fileOutputStream.channel.use { fileChannel ->
                    val byteBuffer = ByteBuffer.wrap(jsonStr.toByteArray())
                    fileChannel.write(byteBuffer)
                }
            }
        } finally {
            jsonLock.unlock()
            FileUtils.delete("$jsonFilePath.bak")
        }
    }

}