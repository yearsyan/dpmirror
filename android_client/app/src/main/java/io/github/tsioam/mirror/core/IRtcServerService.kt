package io.github.tsioam.mirror.core

import android.content.Context

interface IRtcServerService {
    fun isRunning(): Boolean
    fun start()

    companion object {
        fun create(context: Context, wsUrl: String, turn: String): IRtcServerService {
            return ShuzukuRtcServerService(context, wsUrl, turn)
        }
    }
}