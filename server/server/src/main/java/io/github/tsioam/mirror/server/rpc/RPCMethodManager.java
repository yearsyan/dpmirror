package io.github.tsioam.mirror.server.rpc;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import fi.iki.elonen.NanoHTTPD;
import io.github.tsioam.mirror.server.ServerThread;
import io.github.tsioam.mirror.core.device.ConfigurationException;

public class RPCMethodManager {
    private static volatile RPCMethodManager instance;

    public static RPCMethodManager getInstance() {
        if (instance == null) {
            synchronized (RPCMethodManager.class) {
                if (instance == null) {
                    instance = new RPCMethodManager();
                }
            }
        }
        return instance;
    }

    public ConcurrentHashMap<String,IRPCMethod> methodMap = new ConcurrentHashMap<>();
    private final Class<?>[] classes = {
            ListAppMethod.class,
            ScreenConnectMethod.class
    };

    private RPCMethodManager()  {
        for(Class<?> clazz : classes) {
            RPCService service = clazz.getAnnotation(RPCService.class);
            if (service != null && !TextUtils.isEmpty(service.name())) {
                try {
                    IRPCMethod method = (IRPCMethod) clazz.newInstance();
                    methodMap.put(service.name(), method);
                } catch (IllegalAccessException | InstantiationException e) {
                    continue;
                }
            }
        }
    }

    public String callService(ServerThread serverThread, String name, NanoHTTPD.IHTTPSession session, JSONObject bodyData) throws ConfigurationException, IOException {
        if (methodMap.get(name) != null) {
            IRPCMethod method = methodMap.get(name);
            if (method == null) {
                return null;
            }
            return method.onRequest(new IPCContext() {
                @Override
                public JSONObject getData() {
                    return bodyData;
                }

                @Override
                public String getRemoteAddress() {
                    return session.getRemoteIpAddress();
                }

                @Override
                public ServerThread getServer() {
                    return serverThread;
                }
            });
        }
        return null;
    }
}
