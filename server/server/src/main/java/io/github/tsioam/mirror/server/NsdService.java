package io.github.tsioam.mirror.server;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import io.github.tsioam.mirror.server.util.Ln;
import io.github.tsioam.shared.Constants;

public class NsdService {

    private static final String TAG = "NSD";

    private final NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;

    public NsdService() {
        nsdManager = (NsdManager) FakeContext.get().getSystemService(Context.NSD_SERVICE);
    }

    public void registerService() {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        String deviceName = android.os.Build.MANUFACTURER + "_" + android.os.Build.MODEL + "_" + getDeviceId();
        serviceInfo.setServiceName(deviceName);
        serviceInfo.setServiceType(Constants.NSD_SERVICE_TYPE);
        serviceInfo.setPort(Constants.NSD_SERVICE_PORT);

        registrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                Ln.i("Register NSD. Service name: " + deviceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {

            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {

            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {

            }
        };

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    public void unregisterService() {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
        }
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    private String getDeviceId() {
        try {
            return android.os.Build.getSerial();
        } catch (Exception e) {
            return "_1";
        }
    }

}
