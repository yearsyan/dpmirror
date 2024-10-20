package io.github.tsioam.mirror

import android.app.Application

class MirrorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Discovery.initialize(this)
    }
}