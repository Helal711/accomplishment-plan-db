package com.helal.accomplishmentplan.nativeapp

import android.app.Application

class AccomplishmentPlanApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
