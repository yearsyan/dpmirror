package io.github.tsioam.mirror.core

import android.content.Context
import android.system.Os
import android.util.Log
import io.github.tsioam.mirror.IShellServerService
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ShuzukuDaemonServerService(private val context: Context) : IShellServerService.Stub() {

    private var process: Process? = null

    init {
        Log.d("rtc-server", "constructor with Context: context=$context")
    }

    override fun destroy() {
        Log.d("rtc-service", "destroy...")
        process?.destroy()
        System.exit(0)
    }

    override fun exit() {
        destroy()
    }

    override fun launchServer(apkPath: String?, ldPath: String?, wsString: String?, ice: String?) {
        if (apkPath == null) {
            return
        }
        val envMap = System.getenv().toMutableMap()
        envMap.putAll(mapOf(
            "CLASSPATH" to apkPath,
            "LD_LIBRARY_PATH" to ldPath,
            "WS_SERVER_URL" to wsString,
            "TURN_SERVER" to ice
        ))
        val env = envMap.map { (key, value) -> "$key=$value" }.toTypedArray()
        process = Runtime.getRuntime().exec(arrayOf("/system/bin/app_process", "/", "io.github.tsioam.rtcserver.RtcServer"), env, File(apkPath).parentFile)
        if (process == null) {
            return
        }
        val reader = BufferedReader(InputStreamReader(process?.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process?.errorStream))
        val output = StringBuilder()

        Thread({
            reader.useLines { lines -> lines.forEach {
                Log.d("rtc-service-log", it)
                output.appendLine(it)
            }}
            process?.waitFor()
            exit()
        })

        Thread({
            errorReader.useLines { lines -> lines.forEach {
                Log.e("rtc-service-error", it)
            }}
        })
    }

    override fun test(): String {
        return "${Os.getpid()}_${Os.getuid()}_${System.currentTimeMillis()}-${process?.hashCode() ?: -1}"
    }
}