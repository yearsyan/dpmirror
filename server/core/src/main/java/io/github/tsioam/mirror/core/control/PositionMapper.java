package io.github.tsioam.mirror.core.control;

import android.graphics.Rect;

import io.github.tsioam.mirror.core.video.ScreenInfo;
import io.github.tsioam.shared.domain.Point;
import io.github.tsioam.shared.domain.Position;
import io.github.tsioam.shared.domain.Size;

public final class PositionMapper {

    private final Size videoSize;
    private final Rect contentRect;
    private final int coordsRotation;

    public PositionMapper(Size videoSize, Rect contentRect, int videoRotation) {
        this.videoSize = videoSize;
        this.contentRect = contentRect;
        this.coordsRotation = reverseRotation(videoRotation);
    }

    public static PositionMapper from(ScreenInfo screenInfo) {
        // ignore the locked video orientation, the events will apply in coordinates considered in the physical device orientation
        Size videoSize = screenInfo.getUnlockedVideoSize();
        return new PositionMapper(videoSize, screenInfo.getContentRect(), screenInfo.getVideoRotation());
    }

    private static int reverseRotation(int rotation) {
        return (4 - rotation) % 4;
    }

    public Point map(Position position) {
        // reverse the video rotation to apply the events
        Position devicePosition = position.rotate(coordsRotation);

        Size clientVideoSize = devicePosition.getScreenSize();
        if (!videoSize.equals(clientVideoSize)) {
            // The client sends a click relative to a video with wrong dimensions,
            // the device may have been rotated since the event was generated, so ignore the event
            return null;
        }

        Point point = devicePosition.getPoint();
        int convertedX = contentRect.left + point.getX() * contentRect.width() / videoSize.getWidth();
        int convertedY = contentRect.top + point.getY() * contentRect.height() / videoSize.getHeight();
        return new Point(convertedX, convertedY);
    }
}