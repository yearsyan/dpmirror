package io.github.tsioam.shared.request;

import java.io.Serializable;

import io.github.tsioam.shared.video.VideoCodec;

public class ConnectionRequest implements Serializable {

    private int audioBitRate = 128000;
    private int audioSampleRate = 48000;
    private String packageName;
    private String videoCodec = "h265";

    public VideoCodec getVideoCodec() {
        VideoCodec res = VideoCodec.findByName(videoCodec);
        if (res == null) {
            return VideoCodec.H264;
        }
        return res;
    }

}
