package io.github.tsioam.mirror.server;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import io.github.tsioam.mirror.BuildConfig;
import io.github.tsioam.mirror.server.control.ControlChannel;
import io.github.tsioam.mirror.server.control.Controller;
import io.github.tsioam.mirror.server.device.ConfigurationException;
import io.github.tsioam.mirror.server.device.Device;
import io.github.tsioam.mirror.server.device.Streamer;
import io.github.tsioam.mirror.server.util.Ln;
import io.github.tsioam.mirror.server.video.ScreenCapture;
import io.github.tsioam.mirror.server.video.SurfaceCapture;
import io.github.tsioam.mirror.server.video.SurfaceEncoder;
import io.github.tsioam.shared.video.VideoCodec;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import fi.iki.elonen.NanoHTTPD;

public class LoopServer extends NanoHTTPD {

    private ConcurrentHashMap<Session,Boolean> sessionSet = new ConcurrentHashMap();

    public LoopServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String ipAddress = session.getRemoteIpAddress();
        try {
            final Session connectionSession = Session.createAndConnect(ipAddress, 8899);
            sessionSet.put(connectionSession, true);
            connectionSession.setCloseListener(() -> sessionSet.remove(connectionSession));
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", e.toString());
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"code\": 88}");
    }

    public void startLoop() throws IOException {
        start();
        Looper.loop();
    }

    private static FileDescriptor toFd(Socket socket) {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
        return pfd.getFileDescriptor();
    }

    private static class Session {
        private Streamer videoStreamer;
        private final List<Socket> sockets = new ArrayList<>();
        private final List<AsyncProcessor> processors = new ArrayList<>();
        private Runnable closeListener;
        private final Options options;
        private final Device device;

        Session(Options options, Device device) {
            this.device = device;
            this.options = options;
        }

        public static Session createAndConnect(String ipAddress, int port) throws IOException, ConfigurationException {
            Options options = Options.parse(BuildConfig.VERSION_NAME);
            options.setVideoCodec(VideoCodec.AV1);
            Device device = new Device(options);
            Session session = new Session(options, device);
            try {
                session.connectVideoStream(ipAddress, port);
                session.connectControlChannel(ipAddress, port);
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

        private void connectControlChannel(String ipAddress, int port) throws IOException {
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
            Controller controller = new Controller(device, controlChannel, cleanUp, options.getClipboardAutosync(), options.getPowerOn());
            controller.start((err) -> {

            });
            device.setClipboardListener((text) -> {
                Ln.i("clip board" + text);
            });
            processors.add(controller);
        }

        private void connectVideoStream(String ipAddress, int port) throws IOException {
            // connect video stream
            FileDescriptor fd = connectAndWriteHeader(ipAddress, port, (byte) 0);
            videoStreamer = new Streamer(fd, VideoCodec.H265, true, true);
            videoStreamer.writeHandleShakeHeader(0);
            SurfaceCapture surfaceCapture = new ScreenCapture(device);
            SurfaceEncoder encoder = new SurfaceEncoder(surfaceCapture, videoStreamer,  options.getVideoBitRate(), options.getMaxFps(),
                    options.getVideoCodecOptions(), options.getVideoEncoder(), options.getDownsizeOnError());
            encoder.start((err) -> {
                try {
                    cleanup();
                } catch (IOException ignored) {}
            });
            processors.add(encoder);
        }

        private void cleanup() throws IOException {
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
}
