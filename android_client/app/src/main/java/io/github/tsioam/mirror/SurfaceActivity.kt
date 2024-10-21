package io.github.tsioam.mirror

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

const val INTENT_KEY_ADDRESS = "address"
const val INTENT_KEY_PORT = "port"
class SurfaceActivity : ComponentActivity() {
    private lateinit var mirrorContent: MirrorContent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val address = intent.getStringExtra(INTENT_KEY_ADDRESS)
        val port = intent.getIntExtra(INTENT_KEY_PORT, -1)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (address == null || port < 0) {
            setErrorView()
            return
        }
        mirrorContent = MirrorContent(address, port)
        setContentView(mirrorContent.createView(this))
    }

    private fun setErrorView() {

    }

    override fun onPause() {
        super.onPause()
        mirrorContent.onHide()
    }

    override fun onResume() {
        super.onResume()
        mirrorContent.onShow()
    }

    override fun onDestroy() {
        mirrorContent.onDestroy()
        super.onDestroy()
    }

}