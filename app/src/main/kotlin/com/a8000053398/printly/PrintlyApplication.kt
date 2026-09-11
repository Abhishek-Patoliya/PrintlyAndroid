package com.a8000053398.printly

import android.app.Application

/** Holds the application [android.content.Context] for the small set of
 * singleton stores (mirrors each iOS store's own `.shared` singleton) that
 * need file/DataStore access but aren't otherwise part of the Compose
 * dependency graph. */
class PrintlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        lateinit var appContext: Application
            private set
    }
}
