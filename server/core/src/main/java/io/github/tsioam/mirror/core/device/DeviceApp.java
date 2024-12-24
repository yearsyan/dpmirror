package io.github.tsioam.mirror.core.device;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class DeviceApp {

    private final String packageName;
    private final String name;
    private final boolean system;

    public DeviceApp(String packageName, String name, boolean system) {
        this.packageName = packageName;
        this.name = name;
        this.system = system;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public boolean isSystem() {
        return system;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("package_name", packageName);
        jsonObject.put("name", name);
        jsonObject.put("system", system);
        return jsonObject;
    }

    public static JSONArray toJSONArray(List<DeviceApp> list) throws JSONException {
        if (list == null) {
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        for(DeviceApp deviceApp : list) {
            jsonArray.put(deviceApp.toJSON());
        }
        return jsonArray;
    }

    public Drawable getIcon(PackageManager pm) {
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationIcon(applicationInfo);
        } catch (Exception e) {
            return null;
        }
    }
}