package io.github.tsioam.mirror.server;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONObject;

import io.github.tsioam.mirror.core.DeviceSession;
import io.github.tsioam.mirror.server.rpc.RPCMethodManager;
import io.github.tsioam.mirror.core.util.Ln;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import fi.iki.elonen.NanoHTTPD;

public class LoopServer extends NanoHTTPD implements ServerThread {

    private ConcurrentHashMap<DeviceSession,Boolean> sessionSet = new ConcurrentHashMap();

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
    public ConcurrentHashMap<DeviceSession, Boolean> getSessionSet() {
        return sessionSet;
    }
}
