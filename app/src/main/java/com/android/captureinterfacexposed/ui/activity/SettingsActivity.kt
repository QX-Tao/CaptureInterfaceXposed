package com.android.captureinterfacexposed.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.android.captureinterfacexposed.R
import com.android.captureinterfacexposed.application.DefaultApplication
import com.android.captureinterfacexposed.databinding.ActivitySettingsBinding
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity
import com.android.captureinterfacexposed.utils.ShareUtil

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    companion object{
        @JvmStatic
        private val LSP_HOOK = "lsp_hook";
    }

    override fun onCreate() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()

        binding.includeTitleBarSecond.tvTitle.text = getString(R.string.settings)
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener { onBackPressed() }
        binding.includeTitleBarSecond.ivMoreButton.visibility = View.INVISIBLE

    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val switchPreference = findPreference<SwitchPreferenceCompat>(LSP_HOOK)
            switchPreference!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener setOnPreferenceChangeListener@{ _: Preference?, newValue: Any ->
                    val isChecked = newValue as Boolean
                    if (isChecked) {
                        if (!DefaultApplication.isDeviceRooted()) {
                            Toast.makeText(context, "请先授予应用ROOT权限", Toast.LENGTH_SHORT)
                                .show()
                            return@setOnPreferenceChangeListener false
                        }
                    }
                    ShareUtil.putBoolean(context, LSP_HOOK,isChecked)
                    true
                }


        }
    }

}