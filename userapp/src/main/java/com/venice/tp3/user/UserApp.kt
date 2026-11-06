package com.venice.tp3.user

import android.app.Application
import com.venice.tp3.shared.Prefs
import com.venice.tp3.shared.Repo
import com.venice.tp3.shared.User

class UserApp : Application() {
    lateinit var prefs: Prefs
    lateinit var repo: Repo
    var me: User? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = Repo(prefs)
        instance = this
    }

    companion object {
        lateinit var instance: UserApp
        fun app() = instance
    }
}
