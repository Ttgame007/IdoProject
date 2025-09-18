package com.ido.idoprojectapp

import android.llama.cpp.LLamaAndroid
import android.llama.cpp.LLamaAndroid.Companion.instance
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LLMW(private val llamaAndroid: LLamaAndroid = LLamaAndroid.instance()) {
    //borrowed from: https://github.com/AIDE-CH/generative-ai/blob/main/vid12/app/src/main/java/me/learn/javacodellama/LLMW.kt
    interface MessageHandler {
        fun h(msg: String)
    }

    fun load(path: String) {
        GlobalScope.launch {
            llamaAndroid.load(path)
        }
    }


    fun unload() {
        GlobalScope.launch {
            llamaAndroid.unload()
        }
    }

    fun send(msg: String, mh: MessageHandler){
        GlobalScope.launch {
            llamaAndroid.send(msg).collect{mh.h(it)}
        }
    }

}