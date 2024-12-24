package io.github.tsioam.mirror.server.rpc;

import io.github.tsioam.mirror.core.device.Device;
import io.github.tsioam.mirror.core.device.DeviceApp;

@RPCService(name = "listApp")
public class ListAppMethod implements IRPCMethod {
    @Override
    public String onRequest(IPCContext data) {
        try {
            return DeviceApp.toJSONArray(Device.listApps()).toString();
        } catch (Exception e) {
            return "data error " + e.getMessage();
        }
    }
}
