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
        repo = Repo(prefs, admin = false)
        instance = this
    }

    companion object {
        @Volatile
        private var instance: UserApp? = null

        fun app(): UserApp = instance
            ?: throw IllegalStateException("UserApp not initialized - manifest me android:name=\".UserApp\" check karo")
    }
}
