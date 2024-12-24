package io.github.tsioam.mirror.server;

import java.util.concurrent.ConcurrentHashMap;
import io.github.tsioam.mirror.core.DeviceSession;


public interface ServerThread {
    ConcurrentHashMap<DeviceSession,Boolean> getSessionSet();
}
