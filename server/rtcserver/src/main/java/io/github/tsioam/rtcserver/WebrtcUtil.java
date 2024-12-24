package io.github.tsioam.rtcserver;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class WebrtcUtil {
    public static IceCandidate parseIceCandidate(JSONObject jsonObject) throws Exception {

        String sdpMid = jsonObject.getString("sdpMid");
        int sdpMLineIndex = jsonObject.getInt("sdpMLineIndex");
        String sdp = jsonObject.getString("candidate");
        return new IceCandidate(sdpMid, sdpMLineIndex, sdp);
    }

    public static String sessionTypeToString(SessionDescription.Type type) {
        switch (type) {
            case OFFER:
                return "offer";
            case ANSWER:
                return "answer";
            case ROLLBACK:
                return "rollback";
            case PRANSWER:
                return "pranswer";
        }
        return "";
    }

    public static JSONObject sessionDescToJson(SessionDescription sdp) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", sessionTypeToString(sdp.type));
            jsonObject.put("sdp", sdp.description);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }

    public static JSONObject iceCandidateToJson(IceCandidate iceCandidate) {
        JSONObject candidateJson = new JSONObject();
        JSONObject result = new JSONObject();

        try {
            candidateJson.put("sdpMid", iceCandidate.sdpMid);
            candidateJson.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            candidateJson.put("candidate", iceCandidate.sdp);
            result.put("candidate", candidateJson);
            result.put("type", "candidate");

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
