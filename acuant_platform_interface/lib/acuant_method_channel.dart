import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'acuant_platform_interface.dart';

/// An implementation of [AcuantPlatform] that uses method channels.
class MethodChannelAcuant extends AcuantPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('ca.couver.acuantchannel');

  @override
  Future<bool> initialize({
    required String username,
    required String password,
    String? subscription,
  }) async {
    final res = await methodChannel.invokeMethod('INITIALIZE', {
      'username': username,
      'password': password,
      'subscription': subscription ?? '',
    });
    return res;
  }

  @override
  Future showDocumentCamera() async {
    final res = await methodChannel.invokeMethod('SHOW_DOCUMENT_CAMERA');
    return res;
  }

  @override
  Future showFaceCamera() async {
    final res = await methodChannel.invokeMethod('SHOW_FACE_CAMERA');
    return res;
  }

  // @override
  // Future<String?> getPlatformVersion() async {
  //   final version =
  //       await methodChannel.invokeMethod<String>('getPlatformVersion');
  //   return version;
  // }
}
