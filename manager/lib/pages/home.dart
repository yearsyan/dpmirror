import 'package:flutter/material.dart';
import 'package:nsd/nsd.dart';
import 'dart:io';

import '../core/mirror_method_channel.dart';

void showMyDialog(BuildContext context, String content) {
  showDialog(
    context: context,
    builder: (BuildContext context) {
      return AlertDialog(
        title: Text("Apps"),
        content: Text(content),
        actions: <Widget>[
          TextButton(
            child: Text("取消"),
            onPressed: () {
              Navigator.of(context).pop(); // 关闭对话框
            },
          ),
          TextButton(
            child: Text("确认"),
            onPressed: () {
              // 执行确认操作
              Navigator.of(context).pop(); // 关闭对话框
            },
          ),
        ],
      );
    },
  );
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final Set<Service> _services = {};
  Discovery? _discovery;

  Future<void> _initDiscovery() async {
    _discovery = await startDiscovery('_mirror._tcp.');
    _discovery!.addListener(() {

    });
    _discovery!.addServiceListener((service, status) {
      print("add service");
      if (status == ServiceStatus.found) {
        setState(() {
          _services.add(service);
        });
      }
      if (status == ServiceStatus.lost) {
        setState(() {
          _services.removeWhere((item) => item.name == service.name && item.host == service.host);
        });
      }
    });
  }

  @override
  void initState() {
    print("state init ");
    super.initState();
    _initDiscovery();
  }

  @override
  void dispose() {
    super.dispose();
    if (_discovery != null) {
      stopDiscovery(_discovery!);
    }
  }

  @override
  Widget build(BuildContext context) {
    final services = _services.toList();
    return Scaffold(
        appBar: AppBar(
          title: Text("Devices List"),
          actions: Platform.isAndroid ? [IconButton(
            icon: const Icon(Icons.play_arrow),
            onPressed: () {
              Navigator.pushNamed(context, "/run-daemon");
            },
          )] : [],
        ),
        body: Column(
          children: [
            Expanded(
              child: ListView.builder(
                itemCount: services.length,
                padding: const EdgeInsets.all(8),
                itemBuilder: (context, index) {
                  final service = services[index];
                  return Container(
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.all(Radius.circular(6)),
                    ),
                    padding: const EdgeInsets.all(6),
                    child: Column(
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text('${service.host}:${service.port}')
                          ],
                        ),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: service.host != null && service.port != null ? [
                            ElevatedButton(
                              onPressed: () {
                                MethodChannelSingleton().startScreenMirror(service.host!, service.port!, false);
                              },
                              child: const Text("connect"),
                            ),
                            SizedBox(width: 16),
                            ElevatedButton(
                              onPressed: () {
                                final device = DeviceInfo( host: service.host!, port: service.port! );
                                Navigator.pushNamed(context, "/device", arguments: device);
                                //final body = await MethodChannelSingleton().callRpc(service.host!, service.port!, "listApp", "{}");
                              },
                              child: const Text("App"),
                            )
                          ] : [],
                        ),
                      ],
                    )
                  );
                },
              )
            )
          ],
        )
    );
  }
}