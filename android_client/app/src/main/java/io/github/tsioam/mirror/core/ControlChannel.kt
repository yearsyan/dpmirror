package io.github.tsioam.mirror.core

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.LifecycleCoroutineScope
import io.github.tsioam.shared.domain.ControlMessage
import io.github.tsioam.shared.util.Binary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

class ControlChannel(private val outputStream: OutputStream, private val scope: LifecycleCoroutineScope) {
    suspend fun sendTouchEvent(event: MotionEvent, w: Int, h: Int) {
        for(pointerIdx in 0 until  event.pointerCount) {
            val data = ByteBuffer.allocate(32)
            data.put(ControlMessage.TYPE_INJECT_TOUCH_EVENT.toByte())
            data.put(event.action.toByte())
            data.putLong(event.getPointerId(pointerIdx).toLong())
            val x = event.getX(pointerIdx).toInt()
            val y = event.getY(pointerIdx).toInt()
            data.putInt(x)
            data.putInt(y)
            data.putShort(w.toShort())
            data.putShort(h.toShort())
            data.putShort(Binary.floatToU16FixedPoint(event.getPressure(pointerIdx)))
            val actionButton = 0
            val buttons = 0
            data.putInt(actionButton)
            data.putInt(buttons)
            data.flip()
            sendPacket(data.array())
        }
    }

    suspend fun sendKeyEvent(action: Int, keyCode: Int, repeat: Int, metaState: Int) {
        val data = ByteBuffer.allocate(14)
        data.put(ControlMessage.TYPE_INJECT_KEYCODE.toByte())
        data.put(action.toByte())
        data.putInt(keyCode)
        data.putInt(repeat)
        data.putInt(metaState)
        data.flip()
        sendPacket(data.array())
    }

    suspend fun sendReconnectEvent() {
        val data = ByteBuffer.allocate(1)
        data.put(ControlMessage.TYPE_RECONNECT_VIDEO.toByte())
        data.flip()
        sendPacket(data.array())
    }

    suspend fun sendBackOrScreenOn() {
        val data = ByteBuffer.allocate(2)
        data.put(ControlMessage.TYPE_BACK_OR_SCREEN_ON.toByte())
        data.put(KeyEvent.ACTION_UP.toByte())
        data.flip()
        sendPacket(data.array())
    }


    suspend fun sendScreenOn() {
        val data = ByteBuffer.allocate(1)
        data.put(ControlMessage.TYPE_SCREEN_ON.toByte())
        data.flip()
        sendPacket(data.array())
    }

    suspend fun sendAppSwitchKey() {
        sendKeyEvent(MotionEvent.ACTION_DOWN, KeyEvent.KEYCODE_APP_SWITCH, 0, 0)
        sendKeyEvent(MotionEvent.ACTION_UP, KeyEvent.KEYCODE_APP_SWITCH, 0, 0)
    }

    private suspend fun sendPacket(data: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                outputStream.write(data)
            } catch (e: IOException) {
                Log.e("ControlChannel", "exit while writing packet reason: ${e.message}")
            }
        }
    }

}