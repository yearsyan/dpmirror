package io.github.tsioam.mirror.server;

import io.github.tsioam.mirror.server.audio.AudioCapture;
import io.github.tsioam.shared.Constants;
import io.github.tsioam.shared.audio.AudioCodec;
import io.github.tsioam.mirror.server.audio.AudioDirectCapture;
import io.github.tsioam.mirror.server.audio.AudioEncoder;
import io.github.tsioam.mirror.server.audio.AudioPlaybackCapture;
import io.github.tsioam.mirror.server.audio.AudioRawRecorder;
import io.github.tsioam.mirror.server.audio.AudioSource;
import io.github.tsioam.mirror.server.control.ControlChannel;
import io.github.tsioam.mirror.server.control.Controller;
import io.github.tsioam.mirror.server.control.DeviceMessage;
import io.github.tsioam.mirror.server.device.ConfigurationException;
import io.github.tsioam.mirror.server.device.DesktopConnection;
import io.github.tsioam.mirror.server.device.Device;
import io.github.tsioam.mirror.server.device.Streamer;
import io.github.tsioam.mirror.server.util.Ln;
import io.github.tsioam.mirror.server.util.LogUtils;
import io.github.tsioam.mirror.server.util.Settings;
import io.github.tsioam.mirror.server.util.SettingsException;
import io.github.tsioam.mirror.server.video.CameraCapture;
import io.github.tsioam.mirror.server.video.ScreenCapture;
import io.github.tsioam.mirror.server.video.SurfaceCapture;
import io.github.tsioam.mirror.server.video.SurfaceEncoder;
import io.github.tsioam.mirror.server.video.VideoSource;

import android.os.BatteryManager;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Server {

    public static final String SERVER_PATH;

    static {
        String[] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
        // By convention, scrcpy is always executed with the absolute path of scrcpy-server.jar as the first item in the classpath
        SERVER_PATH = classPaths[0];
    }

    public static class Completion {
        private int running;
        private boolean fatalError;

        Completion(int running) {
            this.running = running;
        }

        synchronized void addCompleted(boolean fatalError) {
            --running;
            if (fatalError) {
                this.fatalError = true;
            }
            if (running == 0 || this.fatalError) {
                notify();
            }
        }

        synchronized void await() {
            try {
                while (running > 0 && !fatalError) {
                    wait();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private Server() {
        // not instantiable
    }

    private static void initAndCleanUp(Options options, CleanUp cleanUp) {
        // This method is called from its own thread, so it may only configure cleanup actions which are NOT dynamic (i.e. they are configured once
        // and for all, they cannot be changed from another thread)

        if (options.getShowTouches()) {
            try {
                String oldValue = Settings.getAndPutValue(Settings.TABLE_SYSTEM, "show_touches", "1");
                // If "show touches" was disabled, it must be disabled back on clean up
                if (!"1".equals(oldValue)) {
                    if (!cleanUp.setDisableShowTouches(true)) {
                        Ln.e("Could not disable show touch on exit");
                    }
                }
            } catch (SettingsException e) {
                Ln.e("Could not change \"show_touches\"", e);
            }
        }

        if (options.getStayAwake()) {
            int stayOn = BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_WIRELESS;
            try {
                String oldValue = Settings.getAndPutValue(Settings.TABLE_GLOBAL, "stay_on_while_plugged_in", String.valueOf(stayOn));
                try {
                    int restoreStayOn = Integer.parseInt(oldValue);
                    if (restoreStayOn != stayOn) {
                        // Restore only if the current value is different
                        if (!cleanUp.setRestoreStayOn(restoreStayOn)) {
                            Ln.e("Could not restore stay on on exit");
                        }
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            } catch (SettingsException e) {
                Ln.e("Could not change \"stay_on_while_plugged_in\"", e);
            }
        }

        if (options.getPowerOffScreenOnClose()) {
            if (!cleanUp.setPowerOffScreen(true)) {
                Ln.e("Could not power off screen on exit");
            }
        }
    }

    private static Thread startInitThread(final Options options, final CleanUp cleanUp) {
        Thread thread = new Thread(() -> initAndCleanUp(options, cleanUp), "init-cleanup");
        thread.start();
        return thread;
    }

    public static void main(String... args) {
        looperServerMain();
//        int status = 0;
//        try {
//            internalMain(args);
//        } catch (Throwable t) {
//            Ln.e(t.getMessage(), t);
//            status = 1;
//        } finally {
//            // By default, the Java process exits when all non-daemon threads are terminated.
//            // The Android SDK might start some non-daemon threads internally, preventing the scrcpy server to exit.
//            // So force the process to exit explicitly.
//            System.exit(status);
//        }
    }

    private static void internalMain(String... args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Ln.e("Exception on thread " + t, e);
        });

        Options options = Options.parse(args);

        Ln.disableSystemStreams();
        Ln.initLogLevel(options.getLogLevel());

        Ln.i("Device: [" + Build.MANUFACTURER + "] " + Build.BRAND + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")");

        if (options.getList()) {
            if (options.getCleanup()) {
                CleanUp.unlinkSelf();
            }

            if (options.getListEncoders()) {
                Ln.i(LogUtils.buildVideoEncoderListMessage());
                Ln.i(LogUtils.buildAudioEncoderListMessage());
            }
            if (options.getListDisplays()) {
                Ln.i(LogUtils.buildDisplayListMessage());
            }
            if (options.getListCameras() || options.getListCameraSizes()) {
                Workarounds.apply();
                Ln.i(LogUtils.buildCameraListMessage(options.getListCameraSizes()));
            }
            // Just print the requested data, do not mirror
            return;
        }

//        try {
//            scrcpy(options);
//        } catch (ConfigurationException e) {
//            // Do not print stack trace, a user-friendly error-message has already been logged
//        }
    }


    private static void looperServerMain() {
        Ln.disableSystemStreams();
        Ln.initLogLevel(Ln.Level.VERBOSE);
        Ln.i("starting");

        Workarounds.apply();
        try {
            LoopServer s = new LoopServer(Constants.NSD_SERVICE_PORT);
            NsdService nsdService = new NsdService();
            nsdService.registerService();
            s.startLoop();
        } catch (Exception e) {
            Ln.i("error " + e);
        }
        Ln.i("end");

    }
}
