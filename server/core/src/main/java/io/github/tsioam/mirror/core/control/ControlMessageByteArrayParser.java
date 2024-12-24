package io.github.tsioam.mirror.core.control;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.github.tsioam.shared.domain.ControlMessage;
import io.github.tsioam.shared.domain.Position;
import io.github.tsioam.shared.util.Binary;

public class ControlMessageByteArrayParser {

    public static ControlMessage parse(ByteBuffer dis) throws IOException {
        int type = dis.getChar();
        switch (type) {
            case ControlMessage.TYPE_INJECT_KEYCODE:
                return parseInjectKeycode(dis);
            case ControlMessage.TYPE_INJECT_TEXT:
                //return parseInjectText();
                return null;
            case ControlMessage.TYPE_INJECT_TOUCH_EVENT:
                return parseInjectTouchEvent(dis);
            case ControlMessage.TYPE_INJECT_SCROLL_EVENT:
                return parseInjectScrollEvent(dis);
            case ControlMessage.TYPE_BACK_OR_SCREEN_ON:
                return parseBackOrScreenOnEvent(dis);
            case ControlMessage.TYPE_GET_CLIPBOARD:
                return parseGetClipboard(dis);
            case ControlMessage.TYPE_SET_CLIPBOARD:
                // return parseSetClipboard();
            case ControlMessage.TYPE_SET_SCREEN_POWER_MODE:
                return parseSetScreenPowerMode(dis);
            case ControlMessage.TYPE_EXPAND_NOTIFICATION_PANEL:
            case ControlMessage.TYPE_EXPAND_SETTINGS_PANEL:
            case ControlMessage.TYPE_COLLAPSE_PANELS:
            case ControlMessage.TYPE_ROTATE_DEVICE:
            case ControlMessage.TYPE_OPEN_HARD_KEYBOARD_SETTINGS:
                return ControlMessage.createEmpty(type);
            case ControlMessage.TYPE_UHID_CREATE:
                //return parseUhidCreate();
            case ControlMessage.TYPE_UHID_INPUT:
                //return parseUhidInput();
            case ControlMessage.TYPE_UHID_DESTROY:
                //return parseUhidDestroy();
            case ControlMessage.TYPE_SCREEN_ON:
                return parseScreenOn();
            default:
                throw new ControlProtocolException("Unknown event type: " + type);
        }
    }

    private static ControlMessage parseInjectKeycode(ByteBuffer dis) throws IOException {
        int action = dis.getChar();
        int keycode = dis.getInt();
        int repeat = dis.getInt();
        int metaState = dis.getInt();
        return ControlMessage.createInjectKeycode(action, keycode, repeat, metaState);
    }

    public static ControlMessage parseInjectTouchEvent(ByteBuffer dis) {
        int action = dis.getChar();
        long pointerId = dis.getLong();
        Position position = parsePosition(dis);
        float pressure = Binary.u16FixedPointToFloat(dis.getShort());
        int actionButton = dis.getInt();
        int buttons = dis.getInt();
        return ControlMessage.createInjectTouchEvent(action, pointerId, position, pressure, actionButton, buttons);
    }

    private static Position parsePosition(ByteBuffer dis) {
        int x = dis.getInt();
        int y = dis.getInt();
        int screenWidth = dis.getShort(); // unsigned
        int screenHeight = dis.getShort();
        return new Position(x, y, screenWidth, screenHeight);
    }

    private static ControlMessage parseInjectScrollEvent(ByteBuffer dis) {
        Position position = parsePosition(dis);
        float hScroll = Binary.i16FixedPointToFloat(dis.getShort());
        float vScroll = Binary.i16FixedPointToFloat(dis.getShort());
        int buttons = dis.getInt();
        return ControlMessage.createInjectScrollEvent(position, hScroll, vScroll, buttons);
    }

    private static ControlMessage parseBackOrScreenOnEvent(ByteBuffer dis) {
        int action = dis.getChar();
        return ControlMessage.createBackOrScreenOn(action);
    }

    private static ControlMessage parseGetClipboard(ByteBuffer dis) throws IOException {
        int copyKey = dis.getChar();
        return ControlMessage.createGetClipboard(copyKey);
    }

//    private static ControlMessage parseSetClipboard(ByteBuffer dis) throws IOException {
//        long sequence = dis.getLong();
//        boolean paste = dis.getChar() != 0;
//        String text = parseString();
//        return ControlMessage.createSetClipboard(sequence, text, paste);
//    }

    private static ControlMessage parseSetScreenPowerMode(ByteBuffer dis) {
        int mode = dis.getChar();
        return ControlMessage.createSetScreenPowerMode(mode);
    }

//    private static ControlMessage parseUhidCreate(ByteBuffer dis) throws IOException {
//        int id = dis.getShort();
//        String name = parseString(1);
//        byte[] data = parseByteArray(2);
//        return ControlMessage.createUhidCreate(id, name, data);
//    }

    private static ControlMessage parseScreenOn() throws IOException {
        return ControlMessage.createScreenOn();
    }

}
