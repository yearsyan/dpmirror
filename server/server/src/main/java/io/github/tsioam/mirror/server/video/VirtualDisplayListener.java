package io.github.tsioam.mirror.server.video;

import io.github.tsioam.mirror.server.control.PositionMapper;

public interface VirtualDisplayListener {
    void onNewVirtualDisplay(int displayId, PositionMapper positionMapper);
}
