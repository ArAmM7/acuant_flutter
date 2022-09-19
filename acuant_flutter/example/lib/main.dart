import 'dart:typed_data';

import 'package:flutter/material.dart';
// import 'dart:async';

// import 'package:flutter/services.dart';
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

  AcuantImage? acuantImage;

  @override
  void initState() {
    super.initState();
    // initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  // Future<void> initPlatformState() async {
  //   String platformVersion;
  //   // Platform messages may fail, so we use a try/catch PlatformException.
  //   // We also handle the message potentially returning null.
  //   try {
  //     platformVersion =
  //         await _acuantFlutterPlugin.getPlatformVersion() ?? 'Unknown platform version';
  //   } on PlatformException {
  //     platformVersion = 'Failed to get platform version.';
  //   }

  //   // If the widget was removed from the tree while the asynchronous platform
  //   // message was in flight, we want to discard the reply rather than calling
  //   // setState to update our non-existent appearance.
  //   if (!mounted) return;

  //   setState(() {
  //     _platformVersion = platformVersion;
  //   });
  // }

  void initAcuant() async {
    bool res = await Acuant.instance.initialize(
      username: 'dev_Trulioo_eBFXmb',
      password: 'EHL8uzqkAx4RBKFQ@',
    );
    print(res);
  }

  void showDocumentCamera() async {
    final res = await Acuant.instance.showDocumentCamera();
    print(res);
    if (res is AcuantImage) {
      setState(() {
        acuantImage = res;
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
                if (acuantImage != null) ...[
                  Image.memory(acuantImage!.rawBytes),
                  Text("Aspect ${acuantImage!.aspectRatio}"),
                  Text("DPI ${acuantImage!.dpi}"),
                  Text("Glare ${acuantImage!.glare}"),
                  Text(
                      "isCorrectAspectRatio ${acuantImage!.isCorrectAspectRatio}"),
                  Text("isPassport ${acuantImage!.isPassport}"),
                ]
              ],
            ),
          ),
        ),
      ),
    );
  }
}
