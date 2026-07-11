package com.agentvoice.app.storage

import android.content.Context
import java.util.UUID

class InstallationIdRepository(context: Context) {
    private val preferences = context.getSharedPreferences("agentvoice_installation", Context.MODE_PRIVATE)

    fun getOrCreate(): String {
        preferences.getString(KEY_INSTALLATION_ID, null)?.let { return it }
        return "android_${UUID.randomUUID()}".also { id ->
            preferences.edit().putString(KEY_INSTALLATION_ID, id).apply()
        }
    }

    companion object {
        private const val KEY_INSTALLATION_ID = "installation_id"
    }
}

