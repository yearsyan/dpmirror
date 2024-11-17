package io.github.tsioam.mirror.core

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import io.github.tsioam.mirror.util.clearConfigAndKeyFrameFlags
import io.github.tsioam.mirror.util.isConfigSet
import io.github.tsioam.mirror.util.isKeyFrameSet
import io.github.tsioam.shared.audio.AudioCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer


class AudioPlayer(private val stream: InputStream) {

    private val readThread: Thread
    private val writeThread: Thread
    private val audioBitRate = 128000
    private val sampleRate = 48000
    private lateinit var codec: MediaCodec
    private val audioTrack: AudioTrack

    private val headerBuffer: ByteArray = ByteArray(12)
    private var frameBuffer: ByteArray = ByteArray(4096)

    init {
        val bufferSize =
            AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,  // 如 AudioFormat.CHANNEL_OUT_MONO 或 AudioFormat.CHANNEL_OUT_STEREO
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )
        readThread = Thread({
            val headerByteBuffer = ByteBuffer.wrap(headerBuffer)
            while (!Thread.currentThread().isInterrupted) {

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

                var flag = 0
                if (isConfig) {
                    flag = flag or MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                }
                if (isKeyFrame) {
                    flag = flag or MediaCodec.BUFFER_FLAG_KEY_FRAME
                }

                var frameRead = 0
                while (frameRead != frameSize) {
                    try {
                        frameRead += stream.read(frameBuffer, frameRead, frameSize - frameRead)
                    } catch (e: IOException) {
                        Log.e("Reader", "error read body ${e.message}")
                        return@Thread
                    }
                }

                val inputBufferIndex = codec.dequeueInputBuffer(-1)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(frameBuffer, 0, frameSize)
                        codec.queueInputBuffer(inputBufferIndex, 0, frameSize, pts, flag)
                    }
                }
            }
        })
        writeThread = Thread({
            val bufferInfo = MediaCodec.BufferInfo()
            var pcmData = ByteArray(4096)
            while (!Thread.currentThread().isInterrupted) {
                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, -1)
                while (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                    if (pcmData.size < bufferInfo.size) {
                        pcmData = ByteArray(bufferInfo.size)
                    }
                    outputBuffer[pcmData]
                    outputBuffer.clear()
                    audioTrack.write(pcmData, 0, bufferInfo.size)
                    codec.releaseOutputBuffer(outputIndex, false)
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, -1)
                }
            }
        })
    }

    private fun readUtil(buf: ByteArray) {
        var nRead = 0
        while (nRead != buf.size) {
            try {
                nRead += stream.read(buf, nRead, buf.size - nRead)
            } catch (e: IOException) {
                return
            }
        }
    }

    suspend fun start() {
        val headerBuffer = ByteArray(4)
        withContext(Dispatchers.IO) {
            readUtil(headerBuffer)
        }

        val codecId = ByteBuffer.wrap(headerBuffer).getInt()
        val codecInfo = AudioCodec.findById(codecId)

        codec = MediaCodec.createDecoderByType(codecInfo.mimeType)
        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, codecInfo.mimeType)
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate)
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
        codec.configure(format, null, null, 0)
        audioTrack.play()
        codec.start()
        readThread.start()
        writeThread.start()
    }


    fun exit() {
        readThread.interrupt()
        writeThread.interrupt()
        codec.stop()
        codec.release()
    }
}