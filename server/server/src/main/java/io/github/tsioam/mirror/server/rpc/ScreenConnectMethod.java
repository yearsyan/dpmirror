package io.github.tsioam.mirror.server.rpc;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.IOException;

import io.github.tsioam.mirror.core.DeviceSession;
import io.github.tsioam.mirror.server.ServerThread;
import io.github.tsioam.mirror.core.device.ConfigurationException;
import io.github.tsioam.mirror.core.wrappers.ServiceManager;
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
            final DeviceSession connectionSession = DeviceSession.createAndConnectNewDisplay(context.getRemoteAddress(), 8899, newDisplay, packageName);
            context.getServer().getSessionSet().put(connectionSession, true);
            connectionSession.setCloseListener(() -> context.getServer().getSessionSet().remove(connectionSession));
            return "SUCCESS";
        }

        int displayId = 0;

        if (data.optInt("display_id") > 0) {
            displayId = data.optInt("display_id");
        } else {
            int[] ids = ServiceManager.getDisplayManager().getDisplayIds();
            if (ids.length > 0) {
                displayId = ids[0];
            }
        }
        final DeviceSession connectionSession = DeviceSession.createAndConnect(context.getRemoteAddress(), 8899, displayId);
        context.getServer().getSessionSet().put(connectionSession, true);
        connectionSession.setCloseListener(() -> context.getServer().getSessionSet().remove(connectionSession));
        return "SUCCESS";
    }
}
