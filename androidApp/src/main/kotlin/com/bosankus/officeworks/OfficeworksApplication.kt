package com.bosankus.officeworks

import android.app.Application
import com.bosankus.officeworks.di.initApp

class OfficeworksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initApp(baseUrl = BuildConfig.BASE_URL, isDebug = BuildConfig.DEBUG)
    }
}
