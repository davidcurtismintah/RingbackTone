package com.allow.ringbacktone

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class RBTPreferenceManager(context: Context) {

    companion object {
        private const val USER_PREFS = "user_prefs"
        private const val KEY_RBT_AUDIO_URI = "com.github.dcm.rbt_audio_uri"
    }

    private val prefs: SharedPreferences by lazy { context.getSharedPreferences(USER_PREFS, Application.MODE_PRIVATE) }

    fun setRBTSoundUri(uri: String) {
        prefs.edit().putString(KEY_RBT_AUDIO_URI, uri).apply()
    }

    fun getRBTSoundUri(): String? {
        return prefs.getString(KEY_RBT_AUDIO_URI, null)
    }
}