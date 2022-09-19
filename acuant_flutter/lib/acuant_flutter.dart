// You have generated a new plugin project without specifying the `--platforms`
// flag. A plugin project with no platform support was generated. To add a
// platform, run `flutter create -t plugin --platforms <platforms> .` under the
// same directory. You can also find a detailed instruction on how to add
// platforms in the `pubspec.yaml` at
// https://flutter.dev/docs/development/packages-and-plugins/developing-packages#plugin-platforms.

// import 'acuant_flutter_platform_interface.dart';
library acuant_flutter;

import 'dart:typed_data';

import 'package:acuant_platform_interface/acuant_platform_interface.dart';

class AcuantImage {
  const AcuantImage({
    required this.rawBytes,
    required this.aspectRatio,
    required this.dpi,
    required this.glare,
    required this.isCorrectAspectRatio,
    required this.isPassport,
    required this.sharpness,
  });
  final Uint8List rawBytes;
  final double aspectRatio;
  final int dpi;
  final int glare;
  final bool isCorrectAspectRatio;
  final bool isPassport;
  final int sharpness;

  static AcuantImage? fromMap(Map data) {
    try {
      return AcuantImage(
        rawBytes: data["RAW_BYTES"],
        aspectRatio: data["ASPECT_RATIO"],
        dpi: data["DPI"],
        glare: data["GLARE"],
        isCorrectAspectRatio: data["IS_CORRECT_ASPECT_RATIO"],
        isPassport: data["IS_PASSPORT"],
        sharpness: data["SHARPNESS"],
      );
    } catch (e) {
      return null;
    }
  }
}

class Acuant {
  Acuant._();

  static final Acuant _instance = Acuant._();

  /// get the instance of the [Acuant].
  static Acuant get instance => _instance;

  Future<bool> initialize({
    required String username,
    required String password,
    String? subscription,
  }) {
    return AcuantPlatform.instance.initialize(
      username: username,
      password: password,
      subscription: subscription,
    );
  }

  Future<AcuantImage?> showDocumentCamera() async {
    final res = await AcuantPlatform.instance.showDocumentCamera();
    if (res is Map) {
      return AcuantImage.fromMap(res);
    }
    return null;
  }

  Future<AcuantImage?> showFaceCamera() async {
    final res = await AcuantPlatform.instance.showFaceCamera();
    return res;
    // if (res is Map) {
    //   return AcuantImage.fromMap(res);
    // }
    // return null;
  }
  // Future<String?> getPlatformVersion() {
  //   return AcuantFlutterPlatform.instance.getPlatformVersion();
  // }
}
