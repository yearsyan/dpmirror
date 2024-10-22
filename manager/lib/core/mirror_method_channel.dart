import 'package:flutter/services.dart';

class MethodChannelSingleton {
  static final MethodChannelSingleton _instance = MethodChannelSingleton._internal();
  static const MethodChannel _channel = MethodChannel('io.github.tsioam.mirror');

  MethodChannelSingleton._internal();

  factory MethodChannelSingleton() {
    return _instance;
  }

  Future<void> startScreenMirror(String host, int port, bool audio) async {
    final Map<String,dynamic> args = <String, dynamic>{
      'host': host,
      'port': port,
      'audio': audio
    };
    await _channel.invokeMethod("startScreenMirror", args);
  }
}
