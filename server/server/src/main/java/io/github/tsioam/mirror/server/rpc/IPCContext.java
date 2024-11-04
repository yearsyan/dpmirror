package io.github.tsioam.mirror.server.rpc;

import org.json.JSONObject;

import io.github.tsioam.mirror.server.ServerThread;

public interface IPCContext {
    JSONObject getData();
    String getRemoteAddress();
    ServerThread getServer();
}
