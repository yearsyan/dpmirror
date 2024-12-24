package io.github.tsioam.mirror.core.control;

import android.net.LocalSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.tsioam.shared.domain.ControlMessage;

public final class ControlChannel implements IControlChannel {

    private final ControlMessageReader reader;
    private final DeviceMessageWriter writer;

    public ControlChannel(LocalSocket controlSocket) throws IOException {
        reader = new ControlMessageReader(controlSocket.getInputStream());
        writer = new DeviceMessageWriter(controlSocket.getOutputStream());
    }

    public ControlChannel(InputStream inputStream, OutputStream outputStream) throws IOException {
        reader = new ControlMessageReader(inputStream);
        writer = new DeviceMessageWriter(outputStream);
    }

    public ControlMessage recv() throws IOException {
        return reader.read();
    }

    public void send(DeviceMessage msg) throws IOException {
        writer.write(msg);
    }
}
