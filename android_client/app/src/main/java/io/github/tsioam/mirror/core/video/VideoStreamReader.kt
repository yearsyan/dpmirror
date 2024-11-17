package io.github.tsioam.mirror.core.video

import android.util.Log
import io.github.tsioam.mirror.util.clearConfigAndKeyFrameFlags
import io.github.tsioam.mirror.util.isConfigSet
import io.github.tsioam.mirror.util.isKeyFrameSet
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

private val TAG = "VideoStreamReader"

class VideoStreamReader(
    private val stream: InputStream,
    private val outStream: OutputStream,
    private var onNewFrameListener: (pts: Long, isConfig: Boolean, isKeyFrame: Boolean, data: ByteArray, offset: Int, size: Int) -> Unit
) {
    private lateinit var thread: Thread
    private val headerBuffer: ByteArray = ByteArray(12)
    private var frameBuffer: ByteArray = ByteArray(4096)

    fun start(): Thread {
        thread = Thread({
            val headerByteBuffer = ByteBuffer.wrap(headerBuffer)
            while (!thread.isInterrupted) {
                var headRead = 0
                while (headRead != 12) {
                    try {
                        headRead += stream.read(headerBuffer, headRead, 12 - headRead)
                    } catch (e: IOException) {
                        return@Thread
                    }
                }
                val ptsAndFlags = headerByteBuffer.getLong(0)
                val isKeyFrame = isKeyFrameSet(ptsAndFlags)
                val isConfig = isConfigSet(ptsAndFlags)
                val pts = clearConfigAndKeyFrameFlags(ptsAndFlags)
                val frameSize = headerByteBuffer.getInt(8)
                if (frameBuffer.size < frameSize) {
                    frameBuffer = ByteArray((frameSize + 4095) and (-4096))
                }
                var frameRead = 0
                while (frameRead != frameSize) {
                    try {
                        frameRead += stream.read(frameBuffer, frameRead, frameSize - frameRead)
                    } catch (e: IOException) {
                        Log.e(TAG, "error read body ${e.message}")
                        return@Thread
                    }
                }
                onNewFrameListener(pts, isConfig, isKeyFrame, frameBuffer, 0, frameSize)
            }
        })
        thread.start()
        return thread
    }
}