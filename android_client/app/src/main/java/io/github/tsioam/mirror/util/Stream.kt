package io.github.tsioam.mirror.util

public const val PACKET_FLAG_CONFIG: Long = 1L shl 63
public const val PACKET_FLAG_KEY_FRAME: Long = 1L shl 62
public fun isConfigSet(value: Long): Boolean {
    return (value and PACKET_FLAG_CONFIG) != 0L
}

public fun isKeyFrameSet(value: Long): Boolean {
    return (value and PACKET_FLAG_KEY_FRAME) != 0L
}
public fun clearConfigAndKeyFrameFlags(value: Long): Long {
    val mask = (PACKET_FLAG_CONFIG or PACKET_FLAG_KEY_FRAME).inv()
    return value and mask
}