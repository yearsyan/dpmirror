package io.github.tsioam.mirror

import android.content.Intent
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.tsioam.mirror.ui.theme.MirrorTheme
import io.github.tsioam.mirror.ui.view.RemoteItemCard


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dataList = remember { mutableStateListOf(*Discovery.getInstance().getServices().toTypedArray()) }
            DisposableEffect(Unit) {
                val listener: (services: Map<String, NsdServiceInfo>) -> Unit =  {
                    dataList.clear()
                    dataList.addAll(it.values)
                }

                Discovery.getInstance().registerServiceChangeListener(listener)
                onDispose {
                    Discovery.getInstance().unregisterServiceChangeListener(listener)
                }
            }

            MirrorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column (
                        modifier = Modifier.padding(innerPadding).fillMaxWidth(1.0f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize(1.0f)) {
                            items(dataList) { item ->
                                RemoteItemCard(
                                    item = item,
                                    onConnect = {
                                        openMirrorPage(item)
                                    }
                                )
                            }
                        }
                    }

                }
            }
        }
    }

    private fun openMirrorPage(serviceInfo: NsdServiceInfo) {
        val intent = Intent(this, SurfaceActivity::class.java)
        intent.putExtra(INTENT_KEY_ADDRESS, serviceInfo.host.hostAddress)
        intent.putExtra(INTENT_KEY_PORT, serviceInfo.port)
        startActivity(intent)
    }
}

