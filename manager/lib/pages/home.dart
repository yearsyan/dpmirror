import 'package:flutter/material.dart';
import 'package:nsd/nsd.dart';

import '../core/mirror_method_channel.dart';

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});
  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
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
                          children: [
                            if (service.host != null && service.port != null) ElevatedButton(
                              onPressed: () {
                                MethodChannelSingleton().startScreenMirror(service.host!, service.port!, false);
                              },
                              child: const Text("connect"),
                            ),
                          ],
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