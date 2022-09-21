import 'dart:typed_data';

class AcuantDocumentImage {
  const AcuantDocumentImage({
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

  static AcuantDocumentImage? fromMap(Map data) {
    try {
      return AcuantDocumentImage(
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
