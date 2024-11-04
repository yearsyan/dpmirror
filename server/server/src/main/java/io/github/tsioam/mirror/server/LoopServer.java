package io.github.tsioam.mirror.server;
import android.net.Uri;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.se.omapi.Session;
import android.text.TextUtils;

import org.json.JSONObject;

import io.github.tsioam.mirror.BuildConfig;
import io.github.tsioam.mirror.server.control.ControlChannel;
import io.github.tsioam.mirror.server.control.Controller;
import io.github.tsioam.mirror.server.device.ConfigurationException;
import io.github.tsioam.mirror.server.device.Device;
import io.github.tsioam.mirror.server.device.Streamer;
import io.github.tsioam.mirror.server.rpc.RPCMethodManager;
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

public class LoopServer extends NanoHTTPD implements ServerThread {

    private ConcurrentHashMap<ServerThread.Session,Boolean> sessionSet = new ConcurrentHashMap();

    public LoopServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Uri uri = Uri.parse(session.getUri());
        String path = uri.getPath();
        if (path != null && path.startsWith("/rpc")) {
            JSONObject body = null;
            if (session.getMethod() == Method.POST) {
                try {
                    String lenStr = session.getHeaders().get("content-length");
                    if (!TextUtils.isEmpty(lenStr)) {
                        int len = Integer.parseInt(lenStr);
                        byte[] bodyBytes = new byte[len];
                        int nRead = 0;
                        InputStream inputStream = session.getInputStream();
                        while (nRead < len) {
                            nRead += inputStream.read(bodyBytes, nRead, len - nRead);
                        }
                        body = new JSONObject(new String(bodyBytes));
                        Ln.i("body: " + body);
                    }
                } catch (Exception e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "ERROR: " + e.getMessage());
                }
            }

            String apiName = path.replace("/rpc/", "");
            try {
                String resp = RPCMethodManager.getInstance().callService(this, apiName, session, body);
                if (resp != null) {
                    return newFixedLengthResponse(Response.Status.OK, "application/json", resp);
                }
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/plain", e.getMessage());
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"code\": 88}");
    }

    public void startLoop() throws IOException {
        start();
        Looper.loop();
    }


    @Override
    public ConcurrentHashMap<Session, Boolean> getSessionSet() {
        return sessionSet;
    }
}
