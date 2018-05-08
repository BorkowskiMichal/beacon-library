import 'dart:async';

import 'package:flutter/services.dart';

class BeaconPositionning {
  static const MethodChannel _channel =
      const MethodChannel('beacon_positionning');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
