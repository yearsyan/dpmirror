package io.github.tsioam.mirror.video

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

private const val TAG = "VideoStreamer"
private const val PACKET_FLAG_CONFIG: Long = 1L shl 63
private const val PACKET_FLAG_KEY_FRAME: Long = 1L shl 62
private fun isConfigSet(value: Long): Boolean {
    return (value and PACKET_FLAG_CONFIG) != 0L
}

private fun isKeyFrameSet(value: Long): Boolean {
    return (value and PACKET_FLAG_KEY_FRAME) != 0L
}
private fun clearConfigAndKeyFrameFlags(value: Long): Long {
    val mask = (PACKET_FLAG_CONFIG or PACKET_FLAG_KEY_FRAME).inv()
    return value and mask
}

class VideoStreamReader(
    private val stream: InputStream,
    private var onNewFrameListener: (pts: Long, isConfig: Boolean, isKeyFrame: Boolean, data: ByteArray, offset: Int, size: Int) -> Unit
) {
    private lateinit var thread: Thread
    private val headerBuffer: ByteArray = ByteArray(12)
    private var frameBuffer: ByteArray = ByteArray(4096)

    public fun start(): Thread {
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