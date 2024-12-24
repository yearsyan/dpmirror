package io.github.tsioam.mirror.core.video;

import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.view.Surface;

import io.github.tsioam.mirror.core.AndroidVersions;
import io.github.tsioam.mirror.core.control.PositionMapper;
import io.github.tsioam.mirror.core.device.DisplayInfo;
import io.github.tsioam.shared.domain.NewDisplay;
import io.github.tsioam.mirror.core.util.Ln;
import io.github.tsioam.mirror.core.wrappers.ServiceManager;
import io.github.tsioam.shared.domain.Size;

public class NewDisplayCapture extends SurfaceCapture {

    // Internal fields copied from android.hardware.display.DisplayManager
    private static final int VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH = 1 << 6;
    private static final int VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT = 1 << 7;
    private static final int VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 << 8;
    private static final int VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 << 9;
    private static final int VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 << 10;
    private static final int VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP = 1 << 11;
    private static final int VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED = 1 << 12;
    private static final int VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED = 1 << 13;
    private static final int VIRTUAL_DISPLAY_FLAG_OWN_FOCUS = 1 << 14;
    private static final int VIRTUAL_DISPLAY_FLAG_DEVICE_DISPLAY_GROUP = 1 << 15;

    private final VirtualDisplayListener vdListener;
    private final NewDisplay newDisplay;

    private Size mainDisplaySize;
    private int mainDisplayDpi;
    private int maxSize; // only used if newDisplay.getSize() != null

    private VirtualDisplay virtualDisplay;
    private Size size;
    private int dpi;

    public NewDisplayCapture(VirtualDisplayListener vdListener, NewDisplay newDisplay, int maxSize) {
        this.vdListener = vdListener;
        this.newDisplay = newDisplay;
        this.maxSize = maxSize;
    }

    @Override
    public void init() {
        size = newDisplay.getSize();
        dpi = newDisplay.getDpi();
        if (size == null || dpi == 0) {
            DisplayInfo displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(0);
            if (displayInfo != null) {
                mainDisplaySize = displayInfo.getSize();
                mainDisplayDpi = displayInfo.getDpi();
            } else {
                Ln.w("Main display not found, fallback to 1920x1080 240dpi");
                mainDisplaySize = new Size(1920, 1080);
                mainDisplayDpi = 240;
            }
        }
    }

    @Override
    public void prepare() {
        if (!newDisplay.hasExplicitSize()) {
            size = ScreenInfo.computeVideoSize(mainDisplaySize.getWidth(), mainDisplaySize.getHeight(), maxSize);
        }
        if (!newDisplay.hasExplicitDpi()) {
            dpi = scaleDpi(mainDisplaySize, mainDisplayDpi, size);
        }
    }

    @Override
    public void start(Surface surface) {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        int virtualDisplayId;
        try {
            int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                    | VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH
                    | VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT
                    | VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL
                    | VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS;
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_33_ANDROID_13) {
                flags |= VIRTUAL_DISPLAY_FLAG_TRUSTED
                        | VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP
                        | VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED
                        | VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED;
                if (Build.VERSION.SDK_INT >= AndroidVersions.API_34_ANDROID_14) {
                    flags |= VIRTUAL_DISPLAY_FLAG_OWN_FOCUS
                            | VIRTUAL_DISPLAY_FLAG_DEVICE_DISPLAY_GROUP;
                }
            }
            virtualDisplay = ServiceManager.getDisplayManager()
                    .createNewVirtualDisplay("scrcpy", size.getWidth(), size.getHeight(), dpi, surface, flags);
            virtualDisplayId = virtualDisplay.getDisplay().getDisplayId();
            Ln.i("New display: " + size.getWidth() + "x" + size.getHeight() + "/" + dpi + " (id=" + virtualDisplayId + ")");
        } catch (Exception e) {
            Ln.e("Could not create display", e);
            throw new AssertionError("Could not create display");
        }

        if (vdListener != null) {
            virtualDisplayId = virtualDisplay.getDisplay().getDisplayId();
            Rect contentRect = new Rect(0, 0, size.getWidth(), size.getHeight());
            PositionMapper positionMapper = new PositionMapper(size, contentRect, 0);
            vdListener.onNewVirtualDisplay(virtualDisplayId, positionMapper);
        }
    }

    @Override
    public void release() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    @Override
    public synchronized Size getSize() {
        return size;
    }

    @Override
    public synchronized boolean setMaxSize(int newMaxSize) {
        if (newDisplay.hasExplicitSize()) {
            // Cannot retry with a different size if the display size was explicitly provided
            return false;
        }

        maxSize = newMaxSize;
        return true;
    }

    private static int scaleDpi(Size initialSize, int initialDpi, Size size) {
        int den = initialSize.getMax();
        int num = size.getMax();
        return initialDpi * num / den;
    }
}