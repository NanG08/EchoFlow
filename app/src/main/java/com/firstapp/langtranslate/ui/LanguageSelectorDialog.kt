package com.firstapp.langtranslate.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.data.Language
import com.firstapp.langtranslate.data.SupportedLanguages

class LanguageSelectorDialog(
    private val isSource: Boolean,
    private val onLanguageSelected: (Language) -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_language_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.languageRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = LanguageAdapter(SupportedLanguages.languages) { language ->
            onLanguageSelected(language)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private class LanguageAdapter(
        private val languages: List<Language>,
        private val onItemClick: (Language) -> Unit
    ) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.tvLanguageName)
            val nativeNameText: TextView = view.findViewById(R.id.tvLanguageNativeName)
            val codeText: TextView = view.findViewById(R.id.tvLanguageCode)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_language, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val language = languages[position]
            holder.nameText.text = language.name
            holder.nativeNameText.text = language.nativeName
            holder.codeText.text = language.code.uppercase()

            holder.itemView.setOnClickListener {
                onItemClick(language)
            }
        }

        override fun getItemCount() = languages.size
    }
}
