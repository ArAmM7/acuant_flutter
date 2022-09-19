// import 'package:flutter_test/flutter_test.dart';
// import 'package:acuant_flutter/acuant_flutter.dart';
// import 'package:acuant_flutter/acuant_flutter_platform_interface.dart';
// import 'package:acuant_flutter/acuant_flutter_method_channel.dart';
// import 'package:plugin_platform_interface/plugin_platform_interface.dart';

// class MockAcuantFlutterPlatform 
//     with MockPlatformInterfaceMixin
//     implements AcuantFlutterPlatform {

//   @override
//   Future<String?> getPlatformVersion() => Future.value('42');
// }

// void main() {
//   final AcuantFlutterPlatform initialPlatform = AcuantFlutterPlatform.instance;

//   test('$MethodChannelAcuantFlutter is the default instance', () {
//     expect(initialPlatform, isInstanceOf<MethodChannelAcuantFlutter>());
//   });

//   test('getPlatformVersion', () async {
//     AcuantFlutter acuantFlutterPlugin = AcuantFlutter();
//     MockAcuantFlutterPlatform fakePlatform = MockAcuantFlutterPlatform();
//     AcuantFlutterPlatform.instance = fakePlatform;
  
//     expect(await acuantFlutterPlugin.getPlatformVersion(), '42');
//   });
// }
