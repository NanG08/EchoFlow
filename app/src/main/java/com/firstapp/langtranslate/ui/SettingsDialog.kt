package com.firstapp.langtranslate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.storage.TranslationDatabase
import kotlinx.coroutines.launch

class SettingsDialog : DialogFragment() {

    private lateinit var database: TranslationDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TranslationDatabase(requireContext())

        val switchAutoDetect = view.findViewById<Switch>(R.id.switchAutoDetect)
        val switchContinuous = view.findViewById<Switch>(R.id.switchContinuous)
        val switchConfidence = view.findViewById<Switch>(R.id.switchConfidence)
        val switchHaptic = view.findViewById<Switch>(R.id.switchHaptic)
        val switchDarkMode = view.findViewById<Switch>(R.id.switchDarkMode)
        val btnClearHistory = view.findViewById<TextView>(R.id.btnClearHistory)

        // Load settings
        lifecycleScope.launch {
            val settings = database.loadSettings()
            switchAutoDetect.isChecked = settings["autoDetectLanguage"] as? Boolean ?: true
            switchContinuous.isChecked = settings["continuousMode"] as? Boolean ?: true
            switchConfidence.isChecked = settings["showConfidence"] as? Boolean ?: false
            switchHaptic.isChecked = settings["hapticFeedback"] as? Boolean ?: true
            switchDarkMode.isChecked = settings["darkMode"] as? Boolean ?: false
        }

        // Save settings on change
        switchAutoDetect.setOnCheckedChangeListener { _, isChecked ->
            saveSettingValue("autoDetectLanguage", isChecked)
        }

        switchContinuous.setOnCheckedChangeListener { _, isChecked ->
            saveSettingValue("continuousMode", isChecked)
        }

        switchConfidence.setOnCheckedChangeListener { _, isChecked ->
            saveSettingValue("showConfidence", isChecked)
        }

        switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            saveSettingValue("hapticFeedback", isChecked)
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            saveSettingValue("darkMode", isChecked)
        }

        btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                database.clearHistory()
                dismiss()
            }
        }
    }

    private fun saveSettingValue(key: String, value: Any) {
        lifecycleScope.launch {
            val settings = database.loadSettings().toMutableMap()
            settings[key] = value
            database.saveSettings(settings)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
