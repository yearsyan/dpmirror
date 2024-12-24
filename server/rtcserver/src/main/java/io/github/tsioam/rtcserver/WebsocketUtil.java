package io.github.tsioam.rtcserver;

import com.neovisionaries.ws.client.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.tsioam.mirror.core.util.Ln;

public class WebsocketUtil {
    public static class MessageBuilder {
        private final JSONObject mData;
        public MessageBuilder() {
            mData = new JSONObject();
        }

        public MessageBuilder add(String key, String value) {
            try {
                mData.put(key, value);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public MessageBuilder add(String key, JSONObject value) {
            try {
                mData.put(key, value);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public void sendAsJsonText(WebSocket webSocket) {
            webSocket.sendText(mData.toString());
        }

    }
}
