// You have generated a new plugin project without specifying the `--platforms`
// flag. A plugin project with no platform support was generated. To add a
// platform, run `flutter create -t plugin --platforms <platforms> .` under the
// same directory. You can also find a detailed instruction on how to add
// platforms in the `pubspec.yaml` at
// https://flutter.dev/docs/development/packages-and-plugins/developing-packages#plugin-platforms.

// import 'acuant_flutter_platform_interface.dart';
library acuant_flutter;

import 'package:acuant_platform_interface/acuant_platform_interface.dart';

import 'source/acuant_face_image.dart';
import 'source/acuant_document_image.dart';

export 'source/acuant_face_image.dart';
export 'source/acuant_document_image.dart';
export 'source/enums.dart';

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

  Future<AcuantDocumentImage?> showDocumentCamera({bool isBack = false}) async {
    final res =
        await AcuantPlatform.instance.showDocumentCamera(isBack: isBack);
    if (res is Map) {
      return AcuantDocumentImage.fromMap(res);
    }
    return null;
  }

  Future<AcuantFaceImage?> showFaceCamera() async {
    final res = await AcuantPlatform.instance.showFaceCamera();
    if (res is Map) {
      return AcuantFaceImage.fromMap(res);
    }
    return null;
  }
}
