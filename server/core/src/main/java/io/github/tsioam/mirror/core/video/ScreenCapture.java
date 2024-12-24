package io.github.tsioam.mirror.core.video;

import io.github.tsioam.mirror.core.AndroidVersions;
import io.github.tsioam.mirror.core.control.PositionMapper;
import io.github.tsioam.mirror.core.device.ConfigurationException;
import io.github.tsioam.mirror.core.device.DisplayInfo;
import io.github.tsioam.mirror.core.util.Ln;
import io.github.tsioam.mirror.core.wrappers.ServiceManager;
import io.github.tsioam.mirror.core.wrappers.SurfaceControl;

import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.IBinder;
import android.view.IDisplayFoldListener;
import android.view.IRotationWatcher;
import android.view.Surface;

import io.github.tsioam.shared.domain.Size;


public class ScreenCapture extends SurfaceCapture {

    private final VirtualDisplayListener vdListener;
    private final int displayId;
    private int maxSize;
    private final Rect crop;
    private final int lockVideoOrientation;

    private DisplayInfo displayInfo;
    private ScreenInfo screenInfo;

    private IBinder display;
    private VirtualDisplay virtualDisplay;

    private IRotationWatcher rotationWatcher;
    private IDisplayFoldListener displayFoldListener;

    public ScreenCapture(VirtualDisplayListener vdListener, int displayId, int maxSize, Rect crop, int lockVideoOrientation) {
        this.vdListener = vdListener;
        this.displayId = displayId;
        this.maxSize = maxSize;
        this.crop = crop;
        this.lockVideoOrientation = lockVideoOrientation;
    }

    @Override
    public void init() {
        if (displayId == 0) {
            rotationWatcher = new IRotationWatcher.Stub() {
                @Override
                public void onRotationChanged(int rotation) {
                    requestReset();
                }
            };
            ServiceManager.getWindowManager().registerRotationWatcher(rotationWatcher, displayId);
        }

        if (Build.VERSION.SDK_INT >= AndroidVersions.API_29_ANDROID_10) {
            displayFoldListener = new IDisplayFoldListener.Stub() {

                private boolean first = true;

                @Override
                public void onDisplayFoldChanged(int displayId, boolean folded) {
                    if (first) {
                        // An event is posted on registration to signal the initial state. Ignore it to avoid restarting encoding.
                        first = false;
                        return;
                    }

                    if (ScreenCapture.this.displayId != displayId) {
                        // Ignore events related to other display ids
                        return;
                    }

                    requestReset();
                }
            };
            ServiceManager.getWindowManager().registerDisplayFoldListener(displayFoldListener);
        }
    }

    @Override
    public void prepare() throws ConfigurationException {
        displayInfo = ServiceManager.getDisplayManager().getDisplayInfo(displayId);
        if (displayInfo == null) {
            Ln.e("Display " + displayId + " not found\n");
            throw new ConfigurationException("Unknown display id: " + displayId);
        }

        if ((displayInfo.getFlags() & DisplayInfo.FLAG_SUPPORTS_PROTECTED_BUFFERS) == 0) {
            Ln.w("Display doesn't have FLAG_SUPPORTS_PROTECTED_BUFFERS flag, mirroring can be restricted");
        }

        screenInfo = ScreenInfo.computeScreenInfo(displayInfo.getRotation(), displayInfo.getSize(), crop, maxSize, lockVideoOrientation);
    }

    @Override
    public void start(Surface surface) {
        if (display != null) {
            SurfaceControl.destroyDisplay(display);
            display = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        int virtualDisplayId;
        PositionMapper positionMapper;
        try {
            Size videoSize = screenInfo.getVideoSize();
            virtualDisplay = ServiceManager.getDisplayManager()
                    .createVirtualDisplay("scrcpy", videoSize.getWidth(), videoSize.getHeight(), displayId, surface);
            virtualDisplayId = virtualDisplay.getDisplay().getDisplayId();
            Rect contentRect = new Rect(0, 0, videoSize.getWidth(), videoSize.getHeight());
            // The position are relative to the virtual display, not the original display
            positionMapper = new PositionMapper(videoSize, contentRect, 0);
            Ln.d("Display: using DisplayManager API");
        } catch (Exception displayManagerException) {
            try {
                display = createDisplay();

                Rect contentRect = screenInfo.getContentRect();

                // does not include the locked video orientation
                Rect unlockedVideoRect = screenInfo.getUnlockedVideoSize().toRect();
                int videoRotation = screenInfo.getVideoRotation();
                int layerStack = displayInfo.getLayerStack();

                setDisplaySurface(display, surface, videoRotation, contentRect, unlockedVideoRect, layerStack);
                virtualDisplayId = displayId;
                positionMapper = PositionMapper.from(screenInfo);
                Ln.d("Display: using SurfaceControl API");
            } catch (Exception surfaceControlException) {
                Ln.e("Could not create display using DisplayManager", displayManagerException);
                Ln.e("Could not create display using SurfaceControl", surfaceControlException);
                throw new AssertionError("Could not create display");
            }
        }

        if (vdListener != null) {
            vdListener.onNewVirtualDisplay(virtualDisplayId, positionMapper);
        }
    }

    @Override
    public void release() {
        if (rotationWatcher != null) {
            ServiceManager.getWindowManager().unregisterRotationWatcher(rotationWatcher);
        }
        if (Build.VERSION.SDK_INT >= AndroidVersions.API_29_ANDROID_10) {
            ServiceManager.getWindowManager().unregisterDisplayFoldListener(displayFoldListener);
        }
        if (display != null) {
            SurfaceControl.destroyDisplay(display);
            display = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    @Override
    public Size getSize() {
        return screenInfo.getVideoSize();
    }

    @Override
    public boolean setMaxSize(int newMaxSize) {
        maxSize = newMaxSize;
        return true;
    }

    private static IBinder createDisplay() throws Exception {
        // Since Android 12 (preview), secure displays could not be created with shell permissions anymore.
        // On Android 12 preview, SDK_INT is still R (not S), but CODENAME is "S".
        boolean secure = Build.VERSION.SDK_INT < AndroidVersions.API_30_ANDROID_11 || (Build.VERSION.SDK_INT == AndroidVersions.API_30_ANDROID_11
                && !"S".equals(Build.VERSION.CODENAME));
        return SurfaceControl.createDisplay("scrcpy", secure);
    }

    private static void setDisplaySurface(IBinder display, Surface surface, int orientation, Rect deviceRect, Rect displayRect, int layerStack) {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, orientation, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(display, layerStack);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }
}
