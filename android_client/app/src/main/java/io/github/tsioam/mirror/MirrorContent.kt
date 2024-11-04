package io.github.tsioam.mirror

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.tsioam.mirror.util.Rpc
import io.github.tsioam.mirror.util.await
import io.github.tsioam.mirror.video.VideoStreamReader
import io.github.tsioam.shared.domain.ControlMessage
import io.github.tsioam.shared.domain.NewDisplay
import io.github.tsioam.shared.util.Binary
import io.github.tsioam.shared.video.VideoCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http.HttpMethod
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


private const val TAG = "SurfaceContent"
class MirrorContent(
    private val address: String,
    private val port: Int,
    private val packageName: String?
) {
    private lateinit var activity: ComponentActivity
    private lateinit var surfaceView: SurfaceView
    private val connectionSet: MutableSet<Closeable> = HashSet()
    private var mediaCodec: MediaCodec? = null
    private var serverSocket: ServerSocket? = null
    private var writerThread: Thread? = null
    private var decoderThread: Thread? = null
    private var currentSurface: Surface? = null
    private var showing: Boolean = false
    private var hasInitialized: Boolean = false
    private var videoFormat: MediaFormat? = null
    private var videoMirrorRunning: Boolean = false


    fun createView(activity: ComponentActivity): ViewGroup {
        this.activity = activity
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val parent = activity.window.decorView as ViewGroup
        val view = LayoutInflater.from(activity).inflate(R.layout.surface_container, parent, false) as ViewGroup
        surfaceView = view.findViewById(R.id.surface)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                currentSurface = holder.surface
                if (!hasInitialized && showing) {
                    activity.lifecycleScope.launch {
                        startMirror()
                    }
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopMirror()
            }

        })

        return view
    }

    private fun getDisplay(): NewDisplay? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.intent.getSerializableExtra(INTENT_KEY_DISPLAY, NewDisplay::class.java)
        } else {
            activity.intent.getSerializableExtra(INTENT_KEY_DISPLAY) as NewDisplay
        }
    }

    public fun onHide() {
        showing = false
    }

    public fun onShow() {
        showing = true
        if (!hasInitialized && currentSurface != null) {
            activity.lifecycleScope.launch {
                startMirror()
            }
        } else if (videoMirrorRunning) {
            activity.lifecycleScope.launch(Dispatchers.IO) {
                sendConnectRequest()
            }
        }
    }

    private suspend fun sendConnectRequest() {
        val jo = JSONObject()
        try {
            if (packageName?.isNotEmpty() == true) {
                jo.put("package_name", packageName)
                jo.put("is_app_mirror", true)
                jo.put("display", getDisplay()?.toJSON())
            }
            Rpc.call(address, port, "screen-connect", jo.toString())
        } catch (e: Exception) {
            Toast.makeText(activity, "error",Toast.LENGTH_LONG).show()
            e.printStackTrace()
            // TODO
        }

        Log.e(TAG, "request http success")
    }

    private suspend fun startMirror() = coroutineScope {
        hasInitialized = true
        withContext(Dispatchers.IO) {
            serverSocket = ServerSocket(8899)

            launch {
                sendConnectRequest()
            }

            try {
                while (true) {
                    val socket = serverSocket!!.accept()
                    launch {
                        connectionSet.add(socket)
                        val inputStream = socket.getInputStream()
                        val outputStream = socket.getOutputStream()
                        val headerSize = 12
                        val buf = ByteArray(headerSize)
                        var nHeadRead = 0
                        while (nHeadRead != headerSize) {
                            nHeadRead += inputStream.read(buf, nHeadRead, headerSize - nHeadRead)
                        }
                        val headerBuffer = ByteBuffer.wrap(buf)
                        val connectType = headerBuffer.getInt(0)
                        Log.e(TAG, "connect type$connectType")
                        if (connectType == 0 && currentSurface != null) {
                            handleVideoStream(currentSurface!!, inputStream, outputStream)
                        } else if (connectType == 2) {
                            // control
                            Log.e(TAG, "connect type control")
                            setSurfaceEventHandler(inputStream, outputStream)
                        }
                    }
                    Log.e(TAG, "accept socket success")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {

            }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setSurfaceEventHandler(inputStream: InputStream, outputStream: OutputStream) {
        surfaceView.setOnTouchListener { _, event ->
            Log.d(TAG, "event: ${event}")
            for(pointerIdx in 0 until  event.pointerCount) {
                val data = ByteBuffer.allocate(32)
                data.put(ControlMessage.TYPE_INJECT_TOUCH_EVENT.toByte())
                data.put(event.action.toByte())
                data.putLong(event.getPointerId(pointerIdx).toLong())
                val x = event.getX(pointerIdx).toInt()
                val y = event.getY(pointerIdx).toInt()
                val width = surfaceView.width
                val height = surfaceView.height
                data.putInt(x)
                data.putInt(y)
                data.putShort(width.toShort())
                data.putShort(height.toShort())
                data.putShort(Binary.floatToU16FixedPoint(event.getPressure(pointerIdx)))
                val actionButton = 0
                val buttons = 0
                data.putInt(actionButton)
                data.putInt(buttons)
                data.flip()
                val dataArr = data.array()
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    outputStream.write(dataArr)
                }
            }
            true
        }
    }

    private fun setupMediaCode(codecId: Int, surface: Surface, width: Int, height: Int) {
        val codec = VideoCodec.findById(codecId)
        val mimeType = codec?.mimeType ?: "video/avc"
        Log.d(TAG, "codec mine type: ${mimeType} av1: ${VideoCodec.AV1.mimeType}")
        if (mediaCodec == null) {
            mediaCodec = MediaCodec.createDecoderByType(mimeType)
            val format = MediaFormat.createVideoFormat(mimeType, width, height)
            videoFormat = format
            mediaCodec!!.configure(format, surface, null, 0)
        } else {
            mediaCodec!!.configure(videoFormat, surface, null, 0)
        }
    }

    private suspend fun handleVideoStream(surface: Surface, inputStream: InputStream, outputStream: OutputStream) {
        val header = ByteArray(12)
        Log.e(TAG, "will reading header")
        withContext(Dispatchers.IO) {
            inputStream.read(header)
        }
        val buf = ByteBuffer.wrap(header)
        val codecId = buf.getInt(0)
        val width = buf.getInt(4)
        val height = buf.getInt(8)
        Log.i(TAG, "w:${width} h:${height}")
        // h / w = height / width
        withContext(Dispatchers.Main) {
            Log.i(TAG, "resize surface")
            resizeContent(surfaceView.width, surfaceView.width * height / width)
            Log.i(TAG, "resize surface done")
        }


        Log.i(TAG, "create codec")
        setupMediaCode(codecId, surface, width, height)
        mediaCodec!!.start()
        writerThread = VideoStreamReader(
            stream = inputStream,
            onNewFrameListener = { pts, config, keyFrame, data, offset, size ->
                if (mediaCodec == null || writerThread?.isInterrupted == true) {
                    videoMirrorRunning = false
                    return@VideoStreamReader
                }
                val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(-1)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    inputBuffer?.put(data, offset, size)
                    var flag = 0
                    if (config) {
                        flag = flag or MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                    }
                    if (keyFrame) {
                        flag = flag or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, size, pts, flag)
                }
            }
        ).start()
        decoderThread = Thread({
            val bufferInfo = MediaCodec.BufferInfo()
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo, -1)
                    if (outputBufferIndex >= 0) {
                        mediaCodec!!.releaseOutputBuffer(outputBufferIndex, true)
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        //
                    } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    }
                } catch (e: IllegalStateException) {
                    videoMirrorRunning = false
                    break
                }
            }
        })
        decoderThread!!.start()
        videoMirrorRunning = true
    }

    private suspend fun resizeContent(w: Int, h: Int) {
        surfaceView.layoutParams.apply {
            width = w
            height = h
        }
        surfaceView.requestLayout()
        suspendCoroutine<Unit> { continuation ->
            val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (surfaceView.width == w && surfaceView.height == h) {
                        surfaceView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        continuation.resume(Unit)
                    }
                }
            }
            surfaceView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        }
    }

    fun onDestroy() {
        decoderThread?.interrupt()
        writerThread?.interrupt()
        if (serverSocket != null && !serverSocket!!.isClosed) {
            serverSocket!!.close()
        }
        connectionSet.forEach {
            try {
                it.close()
            } catch (ignored: Exception) {}
        }
        mediaCodec?.reset()
        mediaCodec?.release()
    }

    private fun stopMirror() {
        if (mediaCodec != null) {
            writerThread?.interrupt()
            decoderThread?.interrupt()
            mediaCodec?.stop()
        }
    }
}