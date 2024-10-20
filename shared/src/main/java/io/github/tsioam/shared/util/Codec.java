package io.github.tsioam.shared.util;

public interface Codec {

    enum Type {
        VIDEO,
        AUDIO,
    }

    Type getType();

    int getId();

    String getName();

    String getMimeType();
}
