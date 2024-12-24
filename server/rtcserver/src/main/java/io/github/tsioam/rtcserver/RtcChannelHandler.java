package io.github.tsioam.rtcserver;

import android.graphics.SurfaceTexture;
import android.text.TextUtils;
import android.view.Surface;

import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONObject;
import org.webrtc.CapturerObserver;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.tsioam.mirror.core.AsyncProcessor;
import io.github.tsioam.mirror.core.CleanUp;
import io.github.tsioam.mirror.core.control.Controller;
import io.github.tsioam.mirror.core.control.PositionMapper;
import io.github.tsioam.mirror.core.device.ConfigurationException;
import io.github.tsioam.mirror.core.device.DisplayInfo;
import io.github.tsioam.mirror.core.util.Ln;
import io.github.tsioam.mirror.core.video.ScreenCapture;
import io.github.tsioam.mirror.core.wrappers.ServiceManager;
import io.github.tsioam.shared.domain.Size;

public class RtcChannelHandler implements WebSocketListener, PeerConnection.Observer {
    private WebSocket mWebSocket;
    private final PeerConnectionFactory mPeerConnectionFactory;
    private final String mChannelId;
    private PeerConnection mPeerConnect;
    private VideoSource mVideoSource;
    private CapturerObserver mCapturerObserver;
    private ScreenCapture mScreenCapture;
    private Surface mSurface;
    private final SdpObserver mSdpObserver = new SdpObserver() {

        private SessionDescription mSdp;

        @Override
        public void onCreateSuccess(final SessionDescription sessionDescription) {
            Ln.d("onCreateSuccess - mPeerConnect.setLocalDescription");
            mSdp = new SessionDescription(sessionDescription.type, sessionDescription.description);
            mPeerConnect.setLocalDescription(this, mSdp);
        }

        @Override
        public void onSetSuccess() {
            sendSessionDescription(mSdp);
        }

        @Override
        public void onCreateFailure(String s) {
            Ln.w("create sdp fail" + s);
        }

        @Override
        public void onSetFailure(String s) {
            Ln.d("set sdp fail " + s);
        }
    };

    public RtcChannelHandler(PeerConnectionFactory peerConnectionFactory, String channel) {
        mPeerConnectionFactory = peerConnectionFactory;
        mChannelId = channel;
    }

    private VideoTrack capture(final ControlDataChannel controlDataChannel) {
        final int displayId = 0;
        DisplayInfo displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(displayId);
        Size size = displayInfo.getSize();
        EglBase eglBase = EglBase.create();
        final SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        surfaceTextureHelper.setTextureSize(size.getWidth(), size.getHeight());
        SurfaceTexture surfaceTexture = surfaceTextureHelper.getSurfaceTexture();
        mSurface = new Surface(surfaceTexture);
        mVideoSource = mPeerConnectionFactory.createVideoSource(true);
        mCapturerObserver = mVideoSource.getCapturerObserver();
        mScreenCapture = new ScreenCapture((int newDisplayId, PositionMapper positionMapper) -> {
            Controller controller = new Controller(newDisplayId, controlDataChannel, new CleanUp(null), true, true);
            controller.start(new AsyncProcessor.TerminationListener() {
                @Override
                public void onTerminated(boolean fatalError) {

                }
            });
        }, displayId, 0, null, 0);
        mScreenCapture.init();
        try {
            mScreenCapture.prepare();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        surfaceTextureHelper.startListening(videoFrame -> {
            mCapturerObserver.onFrameCaptured(videoFrame);
        });
        return mPeerConnectionFactory.createVideoTrack(UUID.randomUUID().toString(), mVideoSource);
    }

    private void handleConnect() {
        new WebsocketUtil.MessageBuilder()
                .add("type", "response_new_channel")
                .add("channel", mChannelId)
                .add("device_uuid", RtcServer.DEVICE_UUID)
                .sendAsJsonText(mWebSocket);

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(RtcServerConfig.getInstance().getIceServers());
        mPeerConnect = mPeerConnectionFactory.createPeerConnection(rtcConfig, this);
        Ln.d("will capture");

        DataChannel dataChannel = mPeerConnect.createDataChannel("control", new DataChannel.Init());
        ControlDataChannel controlDataChannel = new ControlDataChannel();
        dataChannel.registerObserver(controlDataChannel);
        VideoTrack videoTrack = capture(controlDataChannel);
        mPeerConnect.addTransceiver(
                videoTrack,
                new RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY)
        );

        MediaConstraints constraints = new MediaConstraints();

        Ln.d("will create offer");
        mPeerConnect.createOffer(mSdpObserver, constraints);
    }

    private void sendSessionDescription(SessionDescription sessionDescription) {
        JSONObject jo = WebrtcUtil.sessionDescToJson(sessionDescription);
        new WebsocketUtil.MessageBuilder()
                .add("type", jo.optString("type"))
                .add("sdp", jo)
                .sendAsJsonText(mWebSocket);
    }

    @Override
    public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {

    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        mWebSocket = websocket;
        handleConnect();
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
        JSONObject data = new JSONObject(text);
        String type = data.optString("type");
        if ("answer".equals(type)) {
            JSONObject sdp = data.optJSONObject("sdp");
            if (sdp == null) {
                return;
            }
            String sdpType = sdp.optString("type");
            String desc = sdp.optString("sdp");
            if (TextUtils.isEmpty(sdpType) || TextUtils.isEmpty(desc) || !"answer".equals(sdpType)) {
                return;
            }
            mPeerConnect.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {

                }

                @Override
                public void onSetSuccess() {
                    Ln.i("set ans success");
                }

                @Override
                public void onCreateFailure(String s) {

                }

                @Override
                public void onSetFailure(String s) {
                    Ln.i("set fail: " + s);
                }
            }, new SessionDescription(SessionDescription.Type.ANSWER, desc));
        } else if ("candidate".equals(type)) {
            JSONObject candidate = data.optJSONObject("candidate");
            if (candidate == null) {
                return;
            }
            mPeerConnect.addIceCandidate(WebrtcUtil.parseIceCandidate(candidate));
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

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Ln.i("onIceCandidate");
        JSONObject candidate = WebrtcUtil.iceCandidateToJson(iceCandidate);
        mWebSocket.sendText(candidate.toString());
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {

    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
        if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
            Ln.i("connect success!!!!");
            mVideoSource.getCapturerObserver().onCapturerStarted(true);
            mScreenCapture.start(mSurface);
        }
    }
}
