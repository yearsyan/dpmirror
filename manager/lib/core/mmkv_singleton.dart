
import 'package:mmkv/mmkv.dart';

class MMKVSingleton {
  static final MMKVSingleton _instance = MMKVSingleton._internal();
  late MMKV _mmkv;

  MMKVSingleton._internal();

  factory MMKVSingleton() {
    return _instance;
  }

  Future<void> insureInit() async {
    await MMKV.initialize();
    _mmkv = MMKV.defaultMMKV();
  }

  MMKV get() {
    return _mmkv;
  }

}