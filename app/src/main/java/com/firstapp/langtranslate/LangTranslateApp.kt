package com.firstapp.langtranslate

import android.app.Application
import com.firstapp.langtranslate.ml.ModelManager

/**
 * EchoFlow Application Class
 * Zero-Latency Voice Translation
 */
class EchoFlowApp : Application() {

    lateinit var modelManager: ModelManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize model manager for on-device ML
        modelManager = ModelManager(this)
    }

    companion object {
        lateinit var instance: EchoFlowApp
            private set
    }
}

// Legacy alias for backward compatibility
typealias LangTranslateApp = EchoFlowApp
