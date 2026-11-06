package com.venice.tp3.admin

import android.app.Application
import com.venice.tp3.shared.Prefs
import com.venice.tp3.shared.Repo

class AdminApp : Application() {
    lateinit var prefs: Prefs
    lateinit var repo: Repo

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = Repo(prefs)
        instance = this
    }

    companion object {
        @Volatile
        private var instance: AdminApp? = null

        fun app(): AdminApp = instance
            ?: throw IllegalStateException("AdminApp not initialized - manifest me android:name=\".AdminApp\" check karo")
    }
}
