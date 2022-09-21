import 'dart:typed_data';

import 'enums.dart';

class AcuantFaceImage {
  const AcuantFaceImage({
    required this.rawBytes,
    required this.liveness,
  });

  final Uint8List rawBytes;
  final AcuantFaceLiveness liveness;

  static AcuantFaceLiveness _livenessFromString(String value) {
    switch (value) {
      case 'LIVE':
        return AcuantFaceLiveness.LIVE;
      case 'NOT_LIVE':
        return AcuantFaceLiveness.NOT_LIVE;
      case 'POOR_QUALITY':
        return AcuantFaceLiveness.POOR_QUALITY;
      default:
        return AcuantFaceLiveness.ERROR;
    }
  }

  static AcuantFaceImage? fromMap(Map data) {
    try {
      return AcuantFaceImage(
        rawBytes: data["RAW_BYTES"],
        liveness: _livenessFromString(data["LIVE"]),
      );
    } catch (e) {
      return null;
    }
  }
}
