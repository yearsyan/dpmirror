package io.github.tsioam.mirror

import android.app.Application
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor

class MirrorApplication : Application() {

    companion object {
        const val FLUTTER_ENGINE_TAG = "warm_up_engine"
    }

    override fun onCreate() {
        super.onCreate()
        val flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )
        FlutterEngineCache
            .getInstance()
            .put(FLUTTER_ENGINE_TAG, flutterEngine)
    }
}