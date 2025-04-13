package io.github.tsioam.mirror.core

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import io.github.tsioam.mirror.BuildConfig
import io.github.tsioam.mirror.IShellServerService
import io.github.tsioam.mirror.MirrorApplication
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import java.io.File


class ShuzukuRtcServerService(
    private val context: Context,
    private val wsUrl: String,
    private val tun: String
) : IRtcServerService {
    private var running: Boolean = false

    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            if (binder.pingBinder()) {
                val service: IShellServerService = IShellServerService.Stub.asInterface(binder)
                try {
                    service.launchServer(
                        File(context.getExternalFilesDir(null), MirrorApplication.SERVER_FILE_NAME).absolutePath,
                        context.applicationInfo.nativeLibraryDir,
                        wsUrl, tun
                    )
                    running = true
                    Log.d("rtc-service", "test: " + service.test())
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            running = false
        }
    }

    public override fun start() {
        if (running) {
            return
        }
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            val userServiceArgs =
                UserServiceArgs(
                    ComponentName(
                        context.packageName,
                        ShuzukuDaemonServerService::class.java.name
                    )
                )
                    .daemon(false)
                    .processNameSuffix("rtc-service")
                    .debuggable(true)
                    .version(BuildConfig.VERSION_CODE)
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        }
    }

    public override fun isRunning(): Boolean {
        return running
    }
}