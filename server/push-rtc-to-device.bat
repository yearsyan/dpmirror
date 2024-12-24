adb push rtcserver/build/outputs/apk/debug/rtcserver-debug.apk /data/local/tmp/rtcserver
adb push rtcserver/build/extracted-libs/debug/lib/arm64-v8a/libjingle_peerconnection_so.so /data/local/tmp/libjingle_peerconnection_so.so
adb shell chmod +x /data/local/tmp/libjingle_peerconnection_so.so
adb shell CLASSPATH=/data/local/tmp/rtcserver LD_LIBRARY_PATH=/data/local/tmp/ WS_SERVER_URL="" ICE_SERVER="" app_process / io.github.tsioam.rtcserver.RtcServer