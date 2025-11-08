package com.travisnguyen.pushpocc.data

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MessageStore {
    private const val PREF_NAME = "push_poc_store"
    private const val KEY_TOKEN = "fcm_token"
    private const val KEY_MESSAGES = "messages"

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withLocale(Locale.US)
        .withZone(ZoneId.systemDefault())

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)

    fun appendMessage(context: Context, title: String?, body: String?, data: Map<String, String>) {
        val timestamp = formatter.format(Instant.now())
        val entry = buildString {
            append("[" + timestamp + "]\n")
            if (!title.isNullOrBlank()) {
                append("Title: $title\n")
            }
            if (!body.isNullOrBlank()) {
                append("Body: $body\n")
            }
            if (data.isNotEmpty()) {
                append("Data: ${data.entries.joinToString()}\n")
            }
        }.trimEnd()

        val existing = prefs(context).getString(KEY_MESSAGES, "")
        val updated = if (existing.isNullOrBlank()) entry else "$entry\n\n$existing"
        prefs(context).edit().putString(KEY_MESSAGES, updated).apply()
    }

    fun getMessages(context: Context): String =
        prefs(context).getString(KEY_MESSAGES, "No messages yet").orEmpty().ifBlank { "No messages yet" }
}
