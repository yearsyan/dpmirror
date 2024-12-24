package io.github.tsioam.rtcserver;

import android.text.TextUtils;

import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

public class RtcServerConfig {

    private static RtcServerConfig sConfig;

    private final String mWsUrl;
    private final List<PeerConnection.IceServer> mIceServers = new ArrayList<>();

    public static RtcServerConfig getInstance() {
        return sConfig;
    }

    private RtcServerConfig() {
        mWsUrl = System.getenv("WS_SERVER_URL");
        if (!TextUtils.isEmpty(System.getenv("ICE_SERVER"))) {
            PeerConnection.IceServer.Builder builder = PeerConnection.IceServer.builder(System.getenv("TURN_SERVER"));
            if (!TextUtils.isEmpty(System.getenv("ICE_USER"))) {
                builder.setUsername(System.getenv("ICE_USER"));
            }
            if (!TextUtils.isEmpty(System.getenv("ICE_PASSWORD"))) {
                builder.setPassword(System.getenv("ICE_PASSWORD"));
            }
            mIceServers.add(builder.createIceServer());
        }
    }

    public static void init() {
        sConfig = new RtcServerConfig();
    }

    public List<PeerConnection.IceServer> getIceServers() {
        return mIceServers;
    }

    public String getWsUrl() {
        return mWsUrl;
    }
}
