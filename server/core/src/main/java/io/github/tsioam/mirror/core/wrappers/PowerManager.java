package io.github.tsioam.mirror.core.wrappers;

import io.github.tsioam.mirror.core.util.Ln;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IInterface;

import java.lang.reflect.Method;

public final class PowerManager {
    private final IInterface manager;
    private Method isScreenOnMethod;

    static PowerManager create() {
        IInterface manager = ServiceManager.getService("power", "android.os.IPowerManager");
        return new PowerManager(manager);
    }

    private PowerManager(IInterface manager) {
        this.manager = manager;
    }

    private Method getIsScreenOnMethod() throws NoSuchMethodException {
        if (isScreenOnMethod == null) {
            @SuppressLint("ObsoleteSdkInt") // we may lower minSdkVersion in the future
            String methodName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH ? "isInteractive" : "isScreenOn";
            isScreenOnMethod = manager.getClass().getMethod(methodName);
        }
        return isScreenOnMethod;
    }

    public boolean isScreenOn() {
        try {
            Method method = getIsScreenOnMethod();
            return (boolean) method.invoke(manager);
        } catch (ReflectiveOperationException e) {
            Ln.e("Could not invoke method", e);
            return false;
        }
    }
}
