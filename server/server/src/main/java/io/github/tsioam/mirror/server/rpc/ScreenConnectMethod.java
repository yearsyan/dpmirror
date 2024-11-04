package io.github.tsioam.mirror.server.rpc;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.IOException;

import io.github.tsioam.mirror.server.ServerThread;
import io.github.tsioam.mirror.server.device.ConfigurationException;
import io.github.tsioam.shared.domain.NewDisplay;

@RPCService(name = "screen-connect")
public class ScreenConnectMethod implements IRPCMethod {
    @Override
    public String onRequest(IPCContext context) throws ConfigurationException, IOException {

        JSONObject data = context.getData();
        if (data.optBoolean("is_app_mirror")) {
            String packageName = data.optString("package_name");
            NewDisplay newDisplay = NewDisplay.fromJSON(data.optJSONObject("display"));
            if (TextUtils.isEmpty(packageName) || newDisplay == null) {
                return "Error";
            }
            final ServerThread.Session connectionSession = ServerThread.Session.createAndConnectNewDisplay(context.getRemoteAddress(), 8899, newDisplay, packageName);
            context.getServer().getSessionSet().put(connectionSession, true);
            connectionSession.setCloseListener(() -> context.getServer().getSessionSet().remove(connectionSession));
            return "SUCCESS";
        }

        final ServerThread.Session connectionSession = ServerThread.Session.createAndConnect(context.getRemoteAddress(), 8899);
        context.getServer().getSessionSet().put(connectionSession, true);
        connectionSession.setCloseListener(() -> context.getServer().getSessionSet().remove(connectionSession));
        return "SUCCESS";
    }
}
