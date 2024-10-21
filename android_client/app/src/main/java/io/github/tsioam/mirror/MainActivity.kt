package io.github.tsioam.mirror

import android.annotation.SuppressLint
import android.content.Intent
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.flutter.embedding.android.FlutterFragment


private const val TAG_FLUTTER_FRAGMENT = "flutter_fragment"
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

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            val dataList = remember { mutableStateListOf(*Discovery.getInstance().getServices().toTypedArray()) }
//            DisposableEffect(Unit) {
//                val listener: (services: Map<String, NsdServiceInfo>) -> Unit =  {
//                    dataList.clear()
//                    dataList.addAll(it.values)
//                }
//
//                Discovery.getInstance().registerServiceChangeListener(listener)
//                onDispose {
//                    Discovery.getInstance().unregisterServiceChangeListener(listener)
//                }
//            }
//
//            MirrorTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Column (
//                        modifier = Modifier.padding(innerPadding).fillMaxWidth(1.0f),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        LazyColumn(modifier = Modifier.fillMaxSize(1.0f)) {
//                            items(dataList) { item ->
//                                RemoteItemCard(
//                                    item = item,
//                                    onConnect = {
//                                        openMirrorPage(item)
//                                    }
//                                )
//                            }
//                        }
//                    }
//
//                }
//            }
//        }
//    }

    private fun openMirrorPage(serviceInfo: NsdServiceInfo) {
        val intent = Intent(this, SurfaceActivity::class.java)
        intent.putExtra(INTENT_KEY_ADDRESS, serviceInfo.host.hostAddress)
        intent.putExtra(INTENT_KEY_PORT, serviceInfo.port)
        startActivity(intent)
    }
}

