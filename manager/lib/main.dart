import 'package:flutter/material.dart';
import 'package:manager/pages/device.dart';
import 'package:manager/pages/home.dart';

void main() => runApp(const MyApp());

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
      },
    );
  }
}


