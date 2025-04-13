import 'package:flutter/material.dart';
import 'package:manager/core/mmkv_singleton.dart';
import 'package:manager/pages/android/run_daemon_android.dart';
import 'package:manager/pages/device.dart';
import 'package:manager/pages/home.dart';

void main() async {
  await MMKVSingleton().insureInit();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mir',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      routes: {
        '/': (context) => const HomePage(),
        '/device': (context) => const DevicePage(),
        '/run-daemon': (context) => const RunDaemonPage()
      },
    );
  }
}


