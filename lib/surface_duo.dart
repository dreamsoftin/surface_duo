import 'dart:async';

import 'package:flutter/services.dart';

class SurfaceDuo {
  static const MethodChannel _channel = const MethodChannel('surface_duo');

  static Future<bool> get isDualScreenDevice async {
    return await _channel.invokeMethod('isDualScreenDevice');
  }

  static Future<bool> get isAppSpanned async {
    return _channel.invokeMethod('isAppSpanned');
  }

  static Future<double> get getHingeAngle async {
    return await _channel.invokeMethod('getHingeAngle');
  }

  static Future<int> get getHingeSize async {
    return await _channel.invokeMethod('getHingeSize');
  }
}
