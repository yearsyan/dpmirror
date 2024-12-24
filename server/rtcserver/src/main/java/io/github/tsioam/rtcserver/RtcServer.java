   package io.github.tsioam.rtcserver;

import android.os.Looper;
import android.text.TextUtils;

import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.PeerConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.github.tsioam.mirror.core.FakeContext;
import io.github.tsioam.mirror.core.Workarounds;
import io.github.tsioam.mirror.core.util.Ln;;

public class RtcServer {

    public static final String DEVICE_UUID = "454ce4bc-9c0a-4965-9828-7b26736dba53";
    private static final String REQ_NEW_CHANNEL = "request_new_channel";
    private final WebSocketFactory webSocketFactory = new WebSocketFactory();

    private void registerToServer(PeerConnectionFactory peerConnectionFactory) throws IOException, WebSocketException {
        final String wsUrl = RtcServerConfig.getInstance().getWsUrl();
        Ln.d("will connect ws" + wsUrl);
        webSocketFactory
                .createSocket(wsUrl)
                .addListener(new WebSocketListener() {
                    @Override
                    public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {

                    }

                    @Override
                    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                        Ln.i("ws connected");
                        new WebsocketUtil.MessageBuilder()
                                .add("type", "server_register")
                                .add("device_uuid", DEVICE_UUID)
                                .sendAsJsonText(websocket);
                    }

                    @Override
                    public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {

                    }

                    @Override
                    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {

                    }

                    @Override
                    public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                    }

                    @Override
                    public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onTextMessage(WebSocket websocket, String text) throws Exception {
                        Ln.i("ws msg: " + text);
                        JSONObject data = new JSONObject(text);
                        if (REQ_NEW_CHANNEL.equals(data.optString("type"))) {
                            final String channel = data.getString("channel");
                            if (!TextUtils.isEmpty(channel)) {
                                webSocketFactory.createSocket(wsUrl)
                                        .addListener(new RtcChannelHandler(peerConnectionFactory, channel))
                                        .connect();
                            }
                        }
                    }

                    @Override
                    public void onTextMessage(WebSocket websocket, byte[] data) throws Exception {

                    }

                    @Override
                    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {

                    }

                    @Override
                    public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

                    }

                    @Override
                    public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

                    }

                    @Override
                    public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

                    }

                    @Override
                    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {

                    }

                    @Override
                    public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {

                    }

                    @Override
                    public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {

                    }

                    @Override
                    public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {

                    }

                    @Override
                    public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

                    }

                    @Override
                    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {

                    }

                    @Override
                    public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {

                    }

                    @Override
                    public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {

                    }
                }).connect();
    }

    private void init() {
        EglBase eglBase = EglBase.createEgl10(EglBase.CONFIG_RGBA);
        // Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(FakeContext.get())
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        // Create a new PeerConnectionFactory instance.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory peerConnectionFactory = PeerConnectionFactory.builder()
                //.setVideoEncoderFactory(new SoftwareVideoEncoderFactory())
                .setVideoEncoderFactory(new HardwareVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setOptions(options)
                .createPeerConnectionFactory();

        Ln.i("will register");

        try {
            registerToServer(peerConnectionFactory);
        } catch (IOException | WebSocketException e) {
            Ln.e("register fail", e);
        }

    }


    public static void main(String... args) {
        Ln.disableSystemStreams();
        Ln.initLogLevel(Ln.Level.VERBOSE);
        Ln.i("Starting");
        Workarounds.apply();
        RtcServerConfig.init();
        RtcServer rtcServer = new RtcServer();
        Ln.i("Create RTC server");
        try {
            new Thread(rtcServer::init).start();
            Looper.loop();
        } catch (Exception e) {
            Ln.e("error init", e);
        }

    }
}
