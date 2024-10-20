package io.github.tsioam.shared.util;

public final class Binary {
    private Binary() {
        // not instantiable
    }

    public static int toUnsigned(short value) {
        return value & 0xffff;
    }

    public static int toUnsigned(byte value) {
        return value & 0xff;
    }

    /**
     * Convert unsigned 16-bit fixed-point to a float between 0 and 1
     *
     * @param value encoded value
     * @return Float value between 0 and 1
     */
    public static float u16FixedPointToFloat(short value) {
        int unsignedShort = Binary.toUnsigned(value);
        // 0x1p16f is 2^16 as float
        return unsignedShort == 0xffff ? 1f : (unsignedShort / 0x1p16f);
    }

    /**
     * Convert signed 16-bit fixed-point to a float between -1 and 1
     *
     * @param value encoded value
     * @return Float value between -1 and 1
     */
    public static float i16FixedPointToFloat(short value) {
        // 0x1p15f is 2^15 as float
        return value == 0x7fff ? 1f : (value / 0x1p15f);
    }

    /**
     * Convert a float between 0 and 1 to an unsigned 16-bit fixed-point value
     *
     * @param value Float value between 0 and 1
     * @return Unsigned 16-bit fixed-point encoded value
     */
    public static short floatToU16FixedPoint(float value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("Value must be between 0 and 1");
        }
        // 0x1p16f is 2^16 as float
        return (short) (value == 1f ? 0xffff : Math.round(value * 0x1p16f));
    }
}
