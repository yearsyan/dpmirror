package io.github.tsioam.mirror.core.video;

import io.github.tsioam.mirror.core.control.PositionMapper;

public interface VirtualDisplayListener {
    void onNewVirtualDisplay(int displayId, PositionMapper positionMapper);
}
