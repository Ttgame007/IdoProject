package com.ido.idoprojectapp

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
    fun send(msg: String, mh: MessageHandler) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            llamaAndroid.send(msg).collect { mh.h(it) }
        }
    }

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

        fun unloadModel() {
            if (isLoaded) {
                instance?.unload()
                isLoaded = false
            }
        }
    }
}