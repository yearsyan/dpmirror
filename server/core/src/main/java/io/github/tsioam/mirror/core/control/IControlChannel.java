package io.github.tsioam.mirror.core.control;

import java.io.IOException;

import io.github.tsioam.shared.domain.ControlMessage;

public interface IControlChannel {
    ControlMessage recv() throws IOException;
    void send(DeviceMessage msg) throws IOException;
}
