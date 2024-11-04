package io.github.tsioam.mirror

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.compose.ui.geometry.Size
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel
import io.github.tsioam.mirror.util.Rpc
import io.github.tsioam.shared.domain.NewDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress


private const val TAG_FLUTTER_FRAGMENT = "flutter_fragment"
private const val FLUTTER_CHANNEL_NAME = "io.github.tsioam.mirror"
class MainActivity : FragmentActivity() {

    private var flutterFragment: FlutterFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val fragmentManager: FragmentManager = supportFragmentManager
        flutterFragment = fragmentManager.findFragmentByTag(TAG_FLUTTER_FRAGMENT) as FlutterFragment?
        if (flutterFragment == null) {
            val newFlutterFragment = FlutterFragment.withCachedEngine(MirrorApplication.FLUTTER_ENGINE_TAG).build<FlutterFragment>()
            flutterFragment = newFlutterFragment
            fragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    newFlutterFragment,
                    TAG_FLUTTER_FRAGMENT
                )
                .commit()
        }
        configMethodChannel()
    }

    private fun configMethodChannel() {
        val flutterEngine = FlutterEngineCache
            .getInstance()
            .get(MirrorApplication.FLUTTER_ENGINE_TAG)
        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, FLUTTER_CHANNEL_NAME).setMethodCallHandler{ call, result ->
            when(call.method) {
                "startScreenMirror" -> {
                    val arguments: Map<String, Any> = call.arguments() ?: return@setMethodCallHandler
                    handleMirrorCall(arguments, false)
                    result.success(null)
                }
                "startAppMirror" -> {
                    val arguments: Map<String, Any> = call.arguments() ?: return@setMethodCallHandler
                    handleMirrorCall(arguments, true)
                    result.success(null)
                }
                "rpcCall" -> {
                    val arguments: Map<String, Any> = call.arguments() ?: return@setMethodCallHandler
                    handleRpcCall(arguments, result)
                }
            }
        }
    }

    private fun handleMirrorCall(arguments: Map<String, Any>, isAppMirror: Boolean) {
        val host = arguments["host"]
        val port = arguments["port"]
        val packageName = arguments["package_name"]
        val displayMetrics = resources.displayMetrics
        val display = NewDisplay.fromMap(arguments) ?: NewDisplay(
            io.github.tsioam.shared.domain.Size(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels
            ), displayMetrics.densityDpi)
        if (host is String && port is Int) {
            lifecycleScope.launch(Dispatchers.IO) {
                val inetAddress: InetAddress = InetAddress.getByName(host)
                val ipAddress = inetAddress.hostAddress
                if (ipAddress != null) {
                    withContext(Dispatchers.Main) {
                        openMirrorPage(ipAddress, port, if (packageName is String && isAppMirror) packageName.toString() else null, display)
                    }
                }
            }
        }
    }

    private fun handleRpcCall(arguments: Map<String, Any>, result: MethodChannel.Result) {
        val method = arguments["method"]
        val body = arguments["body"]
        val host = arguments["host"]
        val port = arguments["port"]
        if (method is String && host is String && port is Int) {
            lifecycleScope.launch(Dispatchers.IO) {
                val inetAddress: InetAddress = InetAddress.getByName(host)
                val ipAddress = inetAddress.hostAddress
                if (ipAddress != null) {
                    result.success(Rpc.call(host, port, method, (body ?: "").toString()))
                    return@launch
                } else {
                    result.error("1", "fail to reslove host", null)
                }
            }
        } else {
            result.error("4", "argument type error", null)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        flutterFragment!!.onPostResume()
    }

    override fun onNewIntent(@NonNull intent: Intent) {
        super.onNewIntent(intent)
        flutterFragment!!.onNewIntent(intent)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        flutterFragment!!.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        flutterFragment!!.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        flutterFragment!!.onActivityResult(
            requestCode,
            resultCode,
            data
        )
    }

    override fun onUserLeaveHint() {
        flutterFragment!!.onUserLeaveHint()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        flutterFragment!!.onTrimMemory(level)
    }

    private fun openMirrorPage(host: String, port: Int, packageName: String?, display: NewDisplay) {
        val intent = Intent(this, SurfaceActivity::class.java)
        intent.putExtra(INTENT_KEY_ADDRESS, host)
        intent.putExtra(INTENT_KEY_PORT, port)
        if (packageName?.isNotEmpty() == true) {
            intent.putExtra(INTENT_KEY_PACKAGE_NAME, packageName)
            intent.putExtra(INTENT_KEY_DISPLAY, display)
        }
        startActivity(intent)
    }
}

