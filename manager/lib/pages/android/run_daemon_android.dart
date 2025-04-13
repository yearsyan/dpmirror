import 'package:flutter/material.dart';
import 'package:manager/core/mmkv_singleton.dart';

import '../../core/mirror_method_channel.dart';

class RunDaemonPage extends StatefulWidget {
  const RunDaemonPage({super.key});

  @override
  State<StatefulWidget> createState() => _RunDaemonPageState();
}

class _RunDaemonPageState extends State<RunDaemonPage> {

  String _tip = "";
  bool _isButtonEnabled = false;
  final TextEditingController _wsUrlController = TextEditingController();
  final TextEditingController _turnController = TextEditingController();

  @override
  void initState() {
    super.initState();
    MethodChannelSingleton().checkAndReqShuzukuPermission().then((res) {
      setState(() {
        _tip = "$res";
      });
    });
    _wsUrlController.addListener(_checkInput);
    _turnController.addListener(_checkInput);
    String? wsUrl = MMKVSingleton().get().decodeString("daemon_rtc_ws_url");
    if (wsUrl?.isNotEmpty ?? false) {
      _wsUrlController.text = wsUrl!;
    }
    String? turn = MMKVSingleton().get().decodeString("daemon_rtc_turn_url");
    if (turn?.isNotEmpty ?? false) {
      _turnController.text = turn!;
    }
  }

  void _checkInput() {
    setState(() {
      _isButtonEnabled = _wsUrlController.text.trim().isNotEmpty && _turnController.text.trim().isNotEmpty;
    });
  }

  @override
  void dispose() {
    _turnController.dispose();
    _wsUrlController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Expanded(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Text("$_tip"),
            TextButton(
              child: const Text("req"),
              onPressed: () async {
                bool res = (await MethodChannelSingleton().checkAndReqShuzukuPermission());
                setState(() {
                  _tip = "$res";
                });
              },
            ),
            TextField(
              controller: _wsUrlController,
              decoration: const InputDecoration(
                labelText: "Websocket URL",
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.text_fields),
              ),
            ),
            SizedBox(height: 16),
            TextField(
              controller: _turnController,
              decoration: const InputDecoration(
                labelText: "TURN/TUN",
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.text_fields),
              ),
            ),
            SizedBox(height: 16),
            TextButton(
              onPressed: _isButtonEnabled ? () async {
                await MethodChannelSingleton().runDaemonServer(_wsUrlController.text, _turnController.text);
                MMKVSingleton().get().encodeString("daemon_rtc_ws_url", _wsUrlController.text);
                MMKVSingleton().get().encodeString("daemon_rtc_turn_url", _turnController.text);
              } : null,
              child: const Text("run server"),
            )
          ],
        ),
      ),
    );
  }

}