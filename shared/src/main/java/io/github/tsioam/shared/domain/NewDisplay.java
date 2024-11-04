package io.github.tsioam.shared.domain;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;

public final class NewDisplay implements Serializable {
    private Size size;
    private int dpi;

    public NewDisplay() {
        // Auto size and dpi
    }

    public NewDisplay(Size size, int dpi) {
        this.size = size;
        this.dpi = dpi;
    }

    public Size getSize() {
        return size;
    }

    public int getDpi() {
        return dpi;
    }

    public boolean hasExplicitSize() {
        return size != null;
    }

    public boolean hasExplicitDpi() {
        return dpi != 0;
    }

    public static NewDisplay fromMap(Map<String, Object> map) {
        NewDisplay display = new NewDisplay();
        if (map.get("width") instanceof Number && map.get("height") instanceof Number) {
            display.size = new Size(((Number)map.get("width")).intValue(), ((Number)map.get("height")).intValue());
        } else {
            return null;
        }
        if (map.get("dpi") instanceof Number) {
            display.dpi = ((Number)map.get("dpi")).intValue();
        } else {
            return null;
        }
        return display;
    }

    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        try {
            jo.put("width", size.getWidth());
            jo.put("height", size.getHeight());
            jo.put("dpi", dpi);
        } catch (Exception e) {
            return jo;
        }
        return jo;
    }

    public static NewDisplay fromJSON(JSONObject jo) {
        if (jo == null) {
            return null;
        }
        NewDisplay display = new NewDisplay();
        display.size = new Size(jo.optInt("width"), jo.optInt("height"));
        display.dpi = jo.optInt("dpi");
        return display;
    }
}