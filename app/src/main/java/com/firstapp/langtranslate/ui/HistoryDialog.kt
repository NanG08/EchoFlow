package com.firstapp.langtranslate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.langtranslate.R
import com.firstapp.langtranslate.data.TranslationResult
import com.firstapp.langtranslate.storage.TranslationDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryDialog : DialogFragment() {

    private lateinit var database: TranslationDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = TranslationDatabase(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        lifecycleScope.launch {
            val history = database.loadHistory()
            recyclerView.adapter = HistoryAdapter(history)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }

    private class HistoryAdapter(
        private val items: List<TranslationResult>
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val originalText: TextView = view.findViewById(R.id.tvHistoryOriginal)
            val translatedText: TextView = view.findViewById(R.id.tvHistoryTranslated)
            val languages: TextView = view.findViewById(R.id.tvHistoryLanguages)
            val timestamp: TextView = view.findViewById(R.id.tvHistoryTimestamp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.originalText.text = item.originalText
            holder.translatedText.text = item.translatedText
            holder.languages.text =
                "${item.sourceLanguage.uppercase()} → ${item.targetLanguage.uppercase()}"
            holder.timestamp.text = dateFormat.format(Date(item.timestamp))
        }

        override fun getItemCount() = items.size
    }
}
