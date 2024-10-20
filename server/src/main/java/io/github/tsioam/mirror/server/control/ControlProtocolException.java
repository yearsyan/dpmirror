package io.github.tsioam.mirror.server.control;

import java.io.IOException;

public class ControlProtocolException extends IOException {
    public ControlProtocolException(String message) {
        super(message);
    }
}
