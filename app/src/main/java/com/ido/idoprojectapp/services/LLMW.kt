package com.ido.idoprojectapp.services

import android.llama.cpp.LLamaAndroid
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LLMW private constructor(
    private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance()
) {
    interface MessageHandler {
        fun h(msg: String)
    }

    // ====== Core Logic ======

    fun load(path: String) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            llamaAndroid.load(path)
        }
    }

    fun unload() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            llamaAndroid.unload()
        }
    }

    fun send(
        msg: String,
        mh: MessageHandler,
        temperature: Float = 0.7f,
        topK: Int = 40,
        topP: Float = 0.9f,
        repeatPenalty: Float = 1.1f
    ) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {

            try {
                llamaAndroid.send(msg).collect { mh.h(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendWithLimit(msg: String, mh: MessageHandler, maxTokens: Int) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            var tokenCount = 0
            llamaAndroid.send(msg).collect { token ->
                tokenCount++
                if (tokenCount <= maxTokens) {
                    mh.h(token)
                } else {
                    return@collect
                }
            }
        }
    }

    // ====== Companion Object ======

    companion object {
        @Volatile private var instance: LLMW? = null

        @JvmStatic
        var isLoaded = false
            private set

        fun getInstance(modelPath: String? = null): LLMW {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = LLMW()
                    }
                }
            }
            if (!isLoaded && modelPath != null) {
                instance!!.load(modelPath)
                isLoaded = true
            }
            return instance!!
        }

        fun getInstance(modelPath: String? = null, contextSize: Int): LLMW {
            return getInstance(modelPath)
        }

        fun unloadModel() {
            if (isLoaded) {
                instance?.unload()
                isLoaded = false
            }
        }
    }
}