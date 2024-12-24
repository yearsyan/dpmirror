package io.github.tsioam.rtcserver;

import org.webrtc.DataChannel;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.tsioam.mirror.core.control.ControlMessageByteArrayParser;
import io.github.tsioam.mirror.core.control.DeviceMessage;
import io.github.tsioam.mirror.core.control.IControlChannel;
import io.github.tsioam.shared.domain.ControlMessage;

public class ControlDataChannel implements DataChannel.Observer, IControlChannel {

    private final BlockingQueue<ControlMessage> messageQueue = new LinkedBlockingQueue<>();

    @Override
    public void onBufferedAmountChange(long l) {

    }

    @Override
    public void onStateChange() {

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        try {
            ControlMessage message = ControlMessageByteArrayParser.parse(buffer.data);
            messageQueue.offer(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ControlMessage recv() throws IOException {
        try {
            return messageQueue.take();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void send(DeviceMessage msg) throws IOException {

    }
}
