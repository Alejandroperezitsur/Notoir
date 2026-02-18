package com.example.notesapp_apv_czg.data

import android.util.Log
import com.google.gson.Gson

interface StructuredLogger {
    fun info(event: String, data: Map<String, String> = emptyMap())
    fun error(event: String, throwable: Throwable? = null, data: Map<String, String> = emptyMap())
}

object AndroidStructuredLogger : StructuredLogger {
    private const val TAG = "Notoir"
    private val gson = Gson()

    override fun info(event: String, data: Map<String, String>) {
        val payload = mapOf("event" to event, "data" to data)
        Log.i(TAG, gson.toJson(payload))
    }

    override fun error(event: String, throwable: Throwable?, data: Map<String, String>) {
        val payload = mapOf("event" to event, "data" to data)
        if (throwable != null) {
            Log.e(TAG, gson.toJson(payload), throwable)
        } else {
            Log.e(TAG, gson.toJson(payload))
        }
    }
}

