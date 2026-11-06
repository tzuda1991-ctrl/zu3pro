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
        lateinit var instance: AdminApp
        fun app() = instance
    }
}
