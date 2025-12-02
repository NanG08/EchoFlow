package com.firstapp.langtranslate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.langtranslate.EchoFlowApp
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.data.TranslationMode
import com.firstapp.langtranslate.ml.TranslationEngine
import kotlinx.coroutines.launch

/**
 * Fragment for manual text entry and translation
 * Allows users to type text and get instant translation
 */
class TextEntryFragment : Fragment() {

    private lateinit var translationEngine: TranslationEngine
    private lateinit var etInputText: EditText
    private lateinit var tvTranslatedText: TextView
    private lateinit var btnTranslate: Button
    private lateinit var btnClear: Button
    private lateinit var tvCharCount: TextView

    private var sourceLanguage = "en"
    private var targetLanguage = "es"

    companion object {
        fun newInstance(srcLang: String, tgtLang: String): TextEntryFragment {
            return TextEntryFragment().apply {
                arguments = Bundle().apply {
                    putString("srcLang", srcLang)
                    putString("tgtLang", tgtLang)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sourceLanguage = it.getString("srcLang", "en")
            targetLanguage = it.getString("tgtLang", "es")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_text_entry, container, false)

        etInputText = view.findViewById(R.id.etInputText)
        tvTranslatedText = view.findViewById(R.id.tvTranslatedText)
        btnTranslate = view.findViewById(R.id.btnTranslate)
        btnClear = view.findViewById(R.id.btnClear)
        tvCharCount = view.findViewById(R.id.tvCharCount)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as EchoFlowApp
        translationEngine = TranslationEngine(requireContext(), app.modelManager)

        setupUI()
    }

    private fun setupUI() {
        // Translate button
        btnTranslate.setOnClickListener {
            val inputText = etInputText.text.toString().trim()
            if (inputText.isNotEmpty()) {
                translateText(inputText)
            }
        }

        // Clear button
        btnClear.setOnClickListener {
            etInputText.text.clear()
            tvTranslatedText.text = ""
            tvCharCount.text = "0 characters"
        }

        // Character counter
        etInputText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCharCount.text = "${s?.length ?: 0} characters"
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun translateText(text: String) {
        btnTranslate.isEnabled = false
        btnTranslate.text = "Translating..."

        lifecycleScope.launch {
            try {
                val result = translationEngine.translate(
                    text = text,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    mode = TranslationMode.TEXT_ENTRY
                )

                tvTranslatedText.text = result.translatedText

            } catch (e: Exception) {
                tvTranslatedText.text = "Error: ${e.message}"
            } finally {
                btnTranslate.isEnabled = true
                btnTranslate.text = "Translate"
            }
        }
    }

    fun updateLanguages(srcLang: String, tgtLang: String) {
        sourceLanguage = srcLang
        targetLanguage = tgtLang
    }
}
