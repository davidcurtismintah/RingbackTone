package com.allow.ringbacktone

import android.app.Application
import android.content.SharedPreferences
import timber.log.Timber
import timber.log.Timber.DebugTree



class RBTApp : Application() {

    val prefs by lazy { RBTPreferenceManager(this) }

    companion object {
        @get:Synchronized
        lateinit var instance: RBTApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}