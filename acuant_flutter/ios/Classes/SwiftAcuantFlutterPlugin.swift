import Flutter
import UIKit
import AVFoundation

import AcuantiOSSDKV11
import AcuantCommon
import AcuantImagePreparation
import AcuantDocumentProcessing
import AcuantFaceMatch
import AcuantHGLiveness
import AcuantPassiveLiveness

public class SwiftAcuantFlutterPlugin: NSObject, FlutterPlugin {
    var mResult: FlutterResult?
    static var mChannel: FlutterMethodChannel?
    var initialized: Bool = false
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "ca.couver.acuantchannel", binaryMessenger: registrar.messenger())
        mChannel = channel
        let instance = SwiftAcuantFlutterPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method != "INITIALIZE" && !initialized {
            result(FlutterError(code: "2", message: "Please initialize first", details: nil))
            return
        }
        if mResult != nil {
            result(FlutterError(code: "3", message: "Please wait for previous task to finish", details: nil))
            return
        }
        switch call.method {
        case "INITIALIZE":
            if initialized {
                result(true)
            } else {
                initAcuant(result: result, call: call)
            }
        case "SHOW_DOCUMENT_CAMERA":
            showCamera(result: result, call: call)
        case "SHOW_FACE_CAMERA":
            showPassiveLiveness(result: result)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
}

extension SwiftAcuantFlutterPlugin {
    func initAcuant(result: @escaping FlutterResult, call: FlutterMethodCall) {
        // Build the params
        let arguments = call.arguments as! [String: Any]
        Credential.setUsername(username: arguments["username"] as? String)
        Credential.setPassword(password: arguments["password"] as? String)
        
        let endpoints = Endpoints()
        endpoints.frmEndpoint = "https://frm.acuant.net"
        endpoints.healthInsuranceEndpoint = "https://medicscan.acuant.net"
        endpoints.idEndpoint = "https://services.assureid.net"
        endpoints.acasEndpoint = "https://acas.acuant.net"
        endpoints.ozoneEndpoint = "https://ozone.acuant.net"
        
        Credential.setEndpoints(endpoints: endpoints)
        
        // End Build the params -------------------------------------------
        
        let initalizer: IAcuantInitializer = AcuantInitializer()
        let packages: [IAcuantPackage] = [ImagePreparationPackage()]
        
        _ = initalizer.initialize(packages: packages) { error in
            DispatchQueue.main.async {
                if let error = error {
                    print(error.errorDescription ?? "Error")
                    self.initialized = false
                    result(false)
                } else {
                    self.initialized = true
                    result(true)
                }
            }
        }
    }
}

extension SwiftAcuantFlutterPlugin {
    @objc func dismissCamVC() {
        if mResult != nil {
            mResult!(FlutterError(code: "0", message: "Cancelled", details: nil))
            mResult = nil
        }
        UIApplication.shared.keyWindow?.rootViewController?.dismiss(animated: true)
    }
    
    func convertImageToBase64String(img: UIImage) -> String {
        return img.jpegData(compressionQuality: 1)?.base64EncodedString() ?? ""
    }
    
    func createBackButton() -> UIButton {
        let button = UIButton(frame: CGRect(x: 15, y: UIApplication.shared.statusBarFrame.height + 15, width: 58, height: 58))
//        button.backgroundColor = .green
        button.setTitle("×", for: .normal)
        button.titleLabel?.font = UIFont.boldSystemFont(ofSize: 32)
        button.addTarget(self, action: #selector(dismissCamVC), for: .touchUpInside)
        return button
    }
    
    func createTitleLabel(title: String) -> UILabel {
        let label = UILabel()
        label.text = title
        label.textColor = .white
        label.font = UIFont.boldSystemFont(ofSize: 18)
        label.sizeToFit()
        // Position it to the right of the back button, vertically centered
        let xPosition: CGFloat = 15 + 58 + 15 // back button x + width + padding
        let buttonYCenter: CGFloat = UIApplication.shared.statusBarFrame.height + 15 + 29 // back button center Y
        let yPosition: CGFloat = buttonYCenter - (label.frame.height / 2)
        label.frame = CGRect(x: xPosition, y: yPosition, width: label.frame.width, height: label.frame.height)
        return label
    }
}

extension SwiftAcuantFlutterPlugin: DocumentCameraViewControllerDelegate {
    public func onCaptured(image: Image, barcodeString: String?) {
        if let capturedImage = image.image {
            ImagePreparation.evaluateImage(data: CroppingData.newInstance(image: image)) { result, error in
                DispatchQueue.main.async {
                    if let evaluatedResult = result {
                        let croppedImage = ImagePreparation.crop(data: CroppingData.newInstance(image: image)).image ?? capturedImage
                        let resizedImage = ImagePreparation.resize(image: croppedImage, targetWidth: 720) ?? croppedImage
                        let compressedImageData = resizedImage.jpegData(compressionQuality: 0.8)
                        var res: [String: Any] = [:]
                        res["RAW_BYTES"] = FlutterStandardTypedData(bytes: compressedImageData!)
                        res["ASPECT_RATIO"] = evaluatedResult.image.size.width / evaluatedResult.image.size.height
                        res["DPI"] = evaluatedResult.dpi
                        res["GLARE"] = evaluatedResult.glare
                        res["IS_CORRECT_ASPECT_RATIO"] = true
                        res["IS_PASSPORT"] = evaluatedResult.isPassport
                        res["SHARPNESS"] = evaluatedResult.sharpness
                        self.mResult?(res)
                        self.mResult = nil
                    } else {
                        self.mResult?(FlutterError(code: "1", message: "Something went wrong", details: error?.errorDescription ?? "Can not find captured image"))
                        self.mResult = nil
                    }
                    self.dismissCamVC()
                }
            }
        } else {
            mResult?(FlutterError(code: "1", message: "Something went wrong", details: "Can not find captured image"))
            mResult = nil
            dismissCamVC()
        }
    }
    
    func showCamera(result: @escaping FlutterResult, call: FlutterMethodCall) {
        mResult = result
        
        // Extract title from arguments
        let arguments = call.arguments as? [String: Any]
        let title = arguments?["title"] as? String ?? ""
        
        let textForState: (DocumentCameraState) -> String = { state in
            switch state {
            case .align: return "ALIGN"
            case .moveCloser: return "MOVE CLOSER"
            case .tooClose: return "TOO CLOSE"
            case .steady: return "HOLD STEADY"
            case .hold: return "HOLD STEADY"
            case .capture: return "CAPTURING"
            @unknown default: return ""
            }
        }
        
        let colorForState: (DocumentCameraState) -> CGColor = { state in
            switch state {
            case .align, .moveCloser, .tooClose: return UIColor.red.cgColor
            case .steady, .hold: return UIColor.yellow.cgColor
            case .capture: return UIColor.green.cgColor
            @unknown default: return UIColor.black.cgColor
            }
        }
        
        let options = DocumentCameraOptions(
            hideNavigationBar: false,
            showBackButton: false,
            autoCapture: true,
            textForState: textForState,
            colorForState: colorForState
        )
        let documentCameraViewController = DocumentCameraViewController(options: options)
        documentCameraViewController.delegate = self
        
        let camNavCtrl = UINavigationController(rootViewController: documentCameraViewController)
        camNavCtrl.view.addSubview(createBackButton())
        
        // Add title label if title is not empty
        if !title.isEmpty {
            camNavCtrl.view.addSubview(createTitleLabel(title: title))
        }
        
        camNavCtrl.modalPresentationStyle = UIModalPresentationStyle.fullScreen
        camNavCtrl.view.backgroundColor = UIColor.black
        UIApplication.shared.keyWindow?.rootViewController?.present(camNavCtrl, animated: true)
    }
}

extension SwiftAcuantFlutterPlugin {
    func showPassiveLiveness(result: @escaping FlutterResult) {
        mResult = result
        DispatchQueue.main.async {
            let faceCameraController = FaceCaptureController()
            faceCameraController.callback = { [weak self] faceCaptureResult in
                if faceCaptureResult != nil {
                    var res: [String: Any] = [:]
                    
                    let workItem = DispatchWorkItem {
                        let croppedImage = ImagePreparation.crop(data: CroppingData.newInstance(image: Image.newInstance(image: faceCaptureResult!.image, data: nil)))
                        // Call the completion handler with the croppedImage
                        print("Cropped Image")
                        DispatchQueue.main.async {
                            self?.handleCroppedImage(croppedImage.image, faceCaptureResult: faceCaptureResult!, res: &res)
                        }
                    }

                    // Execute the work item asynchronously
                    DispatchQueue.global().async(execute: workItem)
                } else {
                    self?.mResult?(FlutterError(code: "1", message: "Something went wrong", details: "Can not find captured image"))
                    self?.mResult = nil
                    self!.dismissCamVC()
                }
            }
            let camNavCtrl = UINavigationController(rootViewController: faceCameraController)
            camNavCtrl.view.addSubview(self.createBackButton())
            camNavCtrl.modalPresentationStyle = .fullScreen
            camNavCtrl.view.backgroundColor = .black
            UIApplication.shared.keyWindow?.rootViewController?.present(camNavCtrl, animated: true)
     
        }
    }

    private func handleCroppedImage(_ croppedImage: UIImage?, faceCaptureResult: FaceCaptureResult, res: inout [String: Any]) {
        let resizedImage = ImagePreparation.resize(image: croppedImage ?? faceCaptureResult.image, targetWidth: 720)
        let compressedImageData = resizedImage!.jpegData(compressionQuality: 0.8)
        res["RAW_BYTES"] = FlutterStandardTypedData(bytes: compressedImageData ?? faceCaptureResult.jpegData)
        res["LIVE"] = "facialLivelinessResultString"
        mResult?(res)
        mResult = nil
        dismissCamVC()
    }
}
