package com.demo.jsonpreviewer

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.captureinterfacexposed.R
import okio.buffer
import okio.source
import java.io.InputStream
import java.nio.charset.Charset

class EditJsonViewModel() : ViewModel() {

    val jsonData: MutableLiveData<String?> = MutableLiveData()

    fun loadConfig(context: Context) {
        var configString: String? = null
        val inputStream: InputStream? =
            context.resources?.openRawResource(R.raw.json_file)
        try {
            configString = inputStream?.source()?.buffer()?.readString(Charset.defaultCharset())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        jsonData.value = configString
    }
}