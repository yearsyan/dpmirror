package io.github.tsioam.mirror

import android.app.Application
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.github.tsioam.mirror.core.IRtcServerService
import io.github.tsioam.mirror.util.copyAssetToFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.sui.Sui
import java.io.File

class MirrorApplication : Application() {

    companion object {
        const val FLUTTER_ENGINE_TAG = "warm_up_engine"
        const val SERVER_FILE_NAME = "rtc-server"
    }

    private var rtcServerServer: IRtcServerService? = null

    public val serverServer: IRtcServerService? get() = rtcServerServer

    override fun onCreate() {
        super.onCreate()
        val flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )
        FlutterEngineCache
            .getInstance()
            .put(FLUTTER_ENGINE_TAG, flutterEngine)
        initAssets()
    }

    private fun initAssets() {
        CoroutineScope(Dispatchers.IO).launch {
            val dataDir = getExternalFilesDir(null) ?: return@launch
            val dist = File(dataDir, SERVER_FILE_NAME)
            if (dist.exists()) {
                return@launch
            }
            copyAssetToFile(applicationContext, SERVER_FILE_NAME, dist.absolutePath)
            dist.setReadOnly()
        }
    }

    public fun runRtcServer(wsUrl: String, turn: String) {
        if (rtcServerServer != null) {
            return
        }
        rtcServerServer = IRtcServerService.create(this, wsUrl, turn)
        rtcServerServer?.start()
    }

    public fun isRtcServerRunning(): Boolean {
        return rtcServerServer?.isRunning() ?: false
    }
}