package io.github.tsioam.mirror.core;

import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.github.tsioam.mirror.core.audio.AudioCapture;
import io.github.tsioam.mirror.core.audio.AudioDirectCapture;
import io.github.tsioam.mirror.core.audio.AudioEncoder;
import io.github.tsioam.mirror.core.audio.AudioSource;
import io.github.tsioam.mirror.core.control.ControlChannel;
import io.github.tsioam.mirror.core.control.Controller;
import io.github.tsioam.mirror.core.device.ConfigurationException;
import io.github.tsioam.mirror.core.device.Device;
import io.github.tsioam.mirror.core.device.Streamer;
import io.github.tsioam.mirror.core.util.Ln;
import io.github.tsioam.mirror.core.video.NewDisplayCapture;
import io.github.tsioam.mirror.core.video.ScreenCapture;
import io.github.tsioam.mirror.core.video.SurfaceCapture;
import io.github.tsioam.mirror.core.video.SurfaceEncoder;
import io.github.tsioam.shared.audio.AudioCodec;
import io.github.tsioam.shared.domain.NewDisplay;
import io.github.tsioam.shared.video.VideoCodec;

public class DeviceSession  {
    private Streamer videoStreamer;
    private final List<Socket> sockets = new ArrayList<>();
    private final List<AsyncProcessor> processors = new ArrayList<>();
    private Runnable closeListener;
    private final Options options;
    private final int displayId;

    public DeviceSession(Options options, int displayId) {
        this.displayId = displayId;
        this.options = options;
    }

    private static FileDescriptor toFd(Socket socket) {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
        return pfd.getFileDescriptor();
    }

    public static DeviceSession createAndConnect(String ipAddress, int port, int displayId) throws IOException, ConfigurationException {
        Options options = Options.parse("VERSION");
        options.setVideoCodec(VideoCodec.H265);
        DeviceSession session = new DeviceSession(options, displayId);
        try {
            Controller controller = session.connectControlChannel(ipAddress, port);
            controller.bindSession(session);
            session.connectVideoStream(controller, null, null, ipAddress, port);
            session.connectAudioSource(ipAddress, port);
        } catch (Exception e){
            session.cleanup();
        }
        return session;
    }

    public static DeviceSession createAndConnectNewDisplay(String ipAddress, int port, NewDisplay display, String packageName) throws IOException, ConfigurationException {
        Options options = Options.parse("VERSION");
        options.setVideoCodec(VideoCodec.H265);
        DeviceSession session = new DeviceSession(options, Device.DISPLAY_ID_NONE);
        try {
            Controller controller = session.connectControlChannel(ipAddress, port);
            controller.bindSession(session);
            session.connectVideoStream(controller, display, packageName, ipAddress, port);
        } catch (Exception e){
            session.cleanup();
        }
        return session;
    }

    public FileDescriptor connectAndWriteHeader(String ipAddress, int port, byte type) throws IOException {
        Socket socket = new Socket(ipAddress, port);
        FileDescriptor fileDescriptor = toFd(socket);


        sockets.add(socket);
        return fileDescriptor;
    }

    public Controller connectControlChannel(String ipAddress, int port) throws IOException {
        Socket socket = new Socket(ipAddress, port);
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(2);
        buffer.putLong(0);
        buffer.flip();
        outputStream.write(buffer.array());

        ControlChannel controlChannel = new ControlChannel(inputStream, outputStream);
        CleanUp cleanUp = CleanUp.configure(options.getDisplayId());
        Controller controller = new Controller(displayId, controlChannel, cleanUp, options.getClipboardAutosync(), options.getPowerOn());
        controller.start((err) -> {
            try {
                cleanup();
            } catch (IOException ignored) {}
        });
        processors.add(controller);
        return controller;
    }

    public void connectVideoStream(Controller controller, NewDisplay newDisplay, String packageName, String ipAddress, int port) throws IOException {
        // connect video stream
        FileDescriptor fd = connectAndWriteHeader(ipAddress, port, (byte) 0);
        videoStreamer = new Streamer(fd, VideoCodec.H265, true, true);
        videoStreamer.writeHandleShakeHeader(0);
        SurfaceCapture surfaceCapture = null;
        if (newDisplay == null) {
            surfaceCapture = new ScreenCapture(controller, options.getDisplayId(), options.getMaxSize(), options.getCrop(), options.getLockVideoOrientation());
        } else {
            surfaceCapture = new NewDisplayCapture((displayId, positionMapper) -> {
                Ln.d("new display capture: " + displayId + " " + packageName);
                Device.startApp(packageName, displayId, true);
                controller.onNewVirtualDisplay(displayId, positionMapper);
            }, newDisplay, options.getMaxSize());
        }
        SurfaceEncoder encoder = new SurfaceEncoder(surfaceCapture, videoStreamer,  options.getVideoBitRate(), options.getMaxFps(),
                options.getVideoCodecOptions(), options.getVideoEncoder(), options.getDownsizeOnError());
        encoder.start((err) -> {});
        processors.add(encoder);
    }

    public void connectAudioSource(String ipAddress, int port) throws IOException {
        FileDescriptor fd = connectAndWriteHeader(ipAddress, port, (byte) 0);
        Streamer streamer = new Streamer(fd, AudioCodec.AAC, true, true);
        streamer.writeHandleShakeHeader(1);
        AudioCapture audioCapture = new AudioDirectCapture(AudioSource.OUTPUT);
        AudioEncoder encoder = new AudioEncoder(audioCapture, streamer, options.getAudioBitRate(), options.getAudioCodecOptions(), options.getAudioEncoder());
        encoder.start((err) -> {});
        processors.add(encoder);
    }

    public void cleanup() throws IOException {
        try {
            for (Socket socket : sockets) {
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    continue;
                }
            }
        } finally {
            if (closeListener != null) {
                closeListener.run();
            }
        }
    }

    public void setCloseListener(Runnable runnable) {
        closeListener = runnable;
    }
}
