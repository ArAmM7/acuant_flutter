import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'acuant_method_channel.dart';

abstract class AcuantPlatform extends PlatformInterface {
  /// Constructs a AcuantPlatform.
  AcuantPlatform() : super(token: _token);

  static final Object _token = Object();

  static AcuantPlatform _instance = MethodChannelAcuant();

  /// The default instance of [AcuantPlatform] to use.
  ///
  /// Defaults to [MethodChannelAcuant].
  static AcuantPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [AcuantPlatform] when
  /// they register themselves.
  static set instance(AcuantPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  // Future<String?> getPlatformVersion() {
  //   throw UnimplementedError('platformVersion() has not been implemented.');
  // }

  Future<bool> initialize({
    required String username,
    required String password,
    String? subscription,
  }) {
    throw UnimplementedError('initialize() has not been implemented.');
  }

  Future showDocumentCamera() {
    throw UnimplementedError('showDocumentCamera() has not been implemented.');
  }

  Future showFaceCamera() {
    throw UnimplementedError('showFaceCamera() has not been implemented.');
  }
}
