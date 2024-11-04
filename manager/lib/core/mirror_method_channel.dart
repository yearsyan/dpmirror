import 'package:flutter/services.dart';

class DeviceInfo {
  DeviceInfo({ required this.port, required this.host });
  final String host;
  final int port;
}

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

  Future<void> startAppMirror(String host, int port, String packageName) async {
    final Map<String,dynamic> args = <String, dynamic>{
      'host': host,
      'port': port,
      'package_name': packageName
    };
    await _channel.invokeMethod("startAppMirror", args);
  }

  Future<String> callRpc(String host, int port, String method, String body) async {
    final Map<String,dynamic> args = <String, dynamic>{
      'host': host,
      'port': port,
      'method': method,
      'body': body
    };
    final res = await _channel.invokeMethod("rpcCall", args);
    return res as String;
  }
}
