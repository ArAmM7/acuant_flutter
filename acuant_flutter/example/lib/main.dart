import 'package:flutter/material.dart';
import 'package:acuant_flutter/acuant_flutter.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  // String _platformVersion = 'Unknown';
  // final _acuantFlutterPlugin = Acuant();

  AcuantDocumentImage? acuantDocumentImage;
  AcuantFaceImage? acuantFaceImage;

  @override
  void initState() {
    super.initState();
  }

  void initAcuant() async {
    bool res = await Acuant.instance.initialize(
      username: 'dev_Trulioo_eBFXmb',
      password: 'EHL8uzqkAx4RBKFQ@',
    );
    print(res);
  }

  void showDocumentCamera() async {
    setState(() {
      acuantDocumentImage = null;
    });
    final res = await Acuant.instance.showDocumentCamera();
    print(res);
    if (res is AcuantDocumentImage) {
      setState(() {
        acuantDocumentImage = res;
      });
    }
  }

  void showFaceCamera() async {
    setState(() {
      acuantFaceImage = null;
    });
    final res = await Acuant.instance.showFaceCamera();
    print(res);
    if (res is AcuantFaceImage) {
      setState(() {
        acuantFaceImage = res;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                ElevatedButton(
                  onPressed: initAcuant,
                  child: Text('initialize'),
                ),
                ElevatedButton(
                  onPressed: showDocumentCamera,
                  child: Text('showDocumentCamera'),
                ),
                ElevatedButton(
                  onPressed: showFaceCamera,
                  child: Text('showFaceCamera'),
                ),
                if (acuantDocumentImage != null) ...[
                  Image.memory(acuantDocumentImage!.rawBytes),
                  Text("Aspect ${acuantDocumentImage!.aspectRatio}"),
                  Text("DPI ${acuantDocumentImage!.dpi}"),
                  Text("Glare ${acuantDocumentImage!.glare}"),
                  Text(
                      "isCorrectAspectRatio ${acuantDocumentImage!.isCorrectAspectRatio}"),
                  Text("isPassport ${acuantDocumentImage!.isPassport}"),
                  Text("Sharpness ${acuantDocumentImage!.sharpness}"),
                ],
                if (acuantFaceImage != null) ...[
                  Image.memory(acuantFaceImage!.rawBytes),
                  Text("Liveness ${acuantFaceImage!.liveness}"),
                ]
              ],
            ),
          ),
        ),
      ),
    );
  }
}
