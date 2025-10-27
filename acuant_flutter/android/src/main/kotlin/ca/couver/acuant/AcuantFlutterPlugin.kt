package ca.couver.acuant

// Basics
import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.annotation.NonNull
import ca.couver.acuantcamera.camera.AcuantCameraActivity
import ca.couver.acuantcamera.camera.AcuantCameraOptions
import ca.couver.acuantcamera.constant.ACUANT_EXTRA_CAMERA_OPTIONS

// Acuant
import ca.couver.acuantcamera.initializer.MrzCameraInitializer
import ca.couver.acuantfacecapture.camera.AcuantFaceCameraActivity
import ca.couver.acuantfacecapture.constant.Constants.ACUANT_EXTRA_FACE_CAPTURE_OPTIONS
import ca.couver.acuantfacecapture.constant.Constants.ACUANT_EXTRA_FACE_IMAGE_URL
import ca.couver.acuantfacecapture.model.CameraMode
import ca.couver.acuantfacecapture.model.FaceCaptureOptions
import com.acuant.acuantcommon.exception.AcuantException
import com.acuant.acuantcommon.initializer.AcuantInitializer
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.acuant.acuantcommon.model.AcuantError
import com.acuant.acuantcommon.model.Credential
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.background.EvaluateImageListener
import com.acuant.acuantimagepreparation.model.AcuantImage
import com.acuant.acuantimagepreparation.model.CroppingData
import com.acuant.acuantpassiveliveness.AcuantPassiveLiveness
import com.acuant.acuantpassiveliveness.model.PassiveLivenessData
import com.acuant.acuantpassiveliveness.model.PassiveLivenessResult
import com.acuant.acuantpassiveliveness.service.PassiveLivenessListener

// Flutter
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream


class AcuantFlutterPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.ActivityResultListener {

    private var mChannel: MethodChannel? = null
    private var mResult: Result? = null
    private var activity: Activity? = null
    private var isInitialized = false
    private var resultSubmitted = false
//    private var processingFacialLiveness = false

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        resultSubmitted = false
        if (call.method != Constants.REQ_INITIALIZE && !isInitialized) {
            result.error("10", "Please initialize first", null)
            return
        }
        
        mResult = result;
        when (call.method) {
            Constants.REQ_INITIALIZE -> {
                val username = call.argument<String>("username")
                val password = call.argument<String>("password")
                val subscription = call.argument<String>("subscription")
                try {
                    initializeAcuantSdk(username, password, subscription)
                    // Don't call result here - let the callback handle it
                } catch (e: AcuantException) {
                    if (!resultSubmitted) {
                        result.error("100", e.localizedMessage, null)
                        resultSubmitted = true
                    }
                }
            }
            Constants.REQ_DOC_CAM -> {
                val isBack = call.argument<Boolean>("isBack")
                showDocumentCapture(isBack ?: false)
            }
            Constants.REQ_FACE_CAM -> {
                showFaceCapture()
            }
            else -> result.notImplemented()
        }
    
    }

    private fun readFromFile(fileUri: String): ByteArray {
        val file = File(fileUri)
        val bytes = ByteArray(file.length().toInt())
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        file.delete()
        return bytes
    }

    private fun handleDocumentCapture(data: Intent?) {
        Log.d("AcuantFlutter", "handleDocumentCapture called, resultSubmitted: $resultSubmitted")
        if (resultSubmitted) return
        
        // Updated for SDK 11.6.0+: Use getLatestCapturedBytes instead of file path
        val bytes = AcuantCameraActivity.getLatestCapturedBytes(clearBytesAfterRead = true)
        Log.d("AcuantFlutter", "Document image bytes: ${bytes?.size}")
        if (bytes == null) {
            mResult?.error("1", "Something went wrong", "Can not find captured image")
            resultSubmitted = true
        } else {
            AcuantImagePreparation.evaluateImage(activity!!, CroppingData(bytes), object :
                EvaluateImageListener {
                override fun onSuccess(image: AcuantImage) {
                    Log.d("AcuantFlutter", "Image evaluation success, resultSubmitted: $resultSubmitted")
                    if (!resultSubmitted) {
                        val bitmap = BitmapFactory.decodeByteArray(image.rawBytes, 0, image.rawBytes.size)
                        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, (720*image.aspectRatio).toInt(), 720, true)
                        val outputStream = ByteArrayOutputStream()
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // Adjust the quality as needed
                        val compressedImageBytes = outputStream.toByteArray()
                        
                        Log.d("AcuantFlutter", "Sending success result with ${compressedImageBytes.size} bytes")
                        mResult?.success(
                            hashMapOf(
                                "RAW_BYTES" to compressedImageBytes,
                                "ASPECT_RATIO" to image.aspectRatio,
                                "DPI" to image.dpi,
                                "GLARE" to image.glare,
                                "IS_CORRECT_ASPECT_RATIO" to image.isCorrectAspectRatio,
                                "IS_PASSPORT" to image.isPassport,
                                "SHARPNESS" to image.sharpness,
                            )
                        )
                        resultSubmitted = true
                    }
                }

                override fun onError(error: AcuantError) {
                    Log.d("AcuantFlutter", "Image evaluation error: ${error.errorDescription}")
                    if (!resultSubmitted) {
                        mResult?.error(
                            error.errorCode.toString(),
                            error.errorDescription,
                            error.additionalDetails
                        )
                        resultSubmitted = true
                    }
                }
            })
        }
    }

    private fun handleFaceCapture(data: Intent?) {
        if (resultSubmitted) return
        
        val url = data?.getStringExtra(ACUANT_EXTRA_FACE_IMAGE_URL)
        if (url == null) {
            mResult?.error("1", "Something went wrong", "Can not find captured image")
            resultSubmitted = true
        } else {
            val bytes = readFromFile(url)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 720, (720 / aspectRatio).toInt(), true)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // Adjust the quality as needed
            val compressedImageBytes = outputStream.toByteArray()
            
            mResult?.success(
                hashMapOf(
                    "RAW_BYTES" to compressedImageBytes,
                    "LIVE" to "facialLivelinessResultString",
                )
            )
            resultSubmitted = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        Log.d("AcuantFlutter", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, resultSubmitted=$resultSubmitted")
        if(resultSubmitted) {
            return false
        }
        when (resultCode) {
            RESULT_OK -> {
                when (requestCode) {
                    Constants.ACT_DOC_CAM_CODE -> {
                        Log.d("AcuantFlutter", "Document camera result received")
                        handleDocumentCapture(data)
                        // Don't set resultSubmitted here - let the async callback handle it
                    }
                    Constants.ACT_FACE_CAM_CODE -> {
                        Log.d("AcuantFlutter", "Face camera result received")
                        handleFaceCapture(data)
                        resultSubmitted = true // Face capture is synchronous
                    }
                    else -> {
                        Log.d("AcuantFlutter", "Unknown request code: $requestCode")
                        mResult?.error(
                            "2",
                            "Not implemented",
                            "It's not a result from camera intent"
                        )
                        resultSubmitted = true
                    }
                }
            }
            else -> {
                Log.d("AcuantFlutter", "Activity result cancelled or failed")
                mResult?.error(
                    "3",
                    "Operation cancelled",
                    "The operation was cancelled or failed"
                )
                resultSubmitted = true
            }
        }
        return true
    }

    private val handleInitialize = object : IAcuantPackageCallback {
        override fun onInitializeSuccess() {
            isInitialized = true;
            if (!resultSubmitted) {
                mResult?.success(true)
                resultSubmitted = true
            }
        }

        override fun onInitializeFailed(errors: List<AcuantError>) {
            isInitialized = false;
            if (!resultSubmitted) {
                var error: AcuantError? = null;
                if (errors.isNotEmpty()) {
                    error = errors[0];
                }
                mResult?.error(
                    error?.errorCode?.toString() ?: "9",
                    error?.errorDescription ?: "Unknown error",
                    null
                )
                resultSubmitted = true
            }
        }
    }


    private fun initializeAcuantSdk(
        username: String?,
        password: String?,
        subscription: String?,
    ) {
        try {
            if (activity == null) {
                if (!resultSubmitted) {
                    mResult?.error("100", "Android Activity not found", null);
                    resultSubmitted = true
                }
            } else if (username == null || password == null) {
                if (!resultSubmitted) {
                    mResult?.error("101", "Username and password are required", null);
                    resultSubmitted = true
                }
            } else {
                Credential.init(
                    username,
                    password,
                    subscription,
                    "https://us.acas.acuant.net",
                    "https://services.assureid.net",
                    "https://frm.acuant.net",
                    "https://us.passlive.acuant.net",
                    null, // ipLivenessEndpoint (removed in v11.6.3)
                    "https://ozone.acuant.net",
                    "https://medicscan.acuant.net"
                )

                AcuantInitializer.initialize(
                    null,
                    activity!!,
                    listOf(MrzCameraInitializer()),
                    handleInitialize
                )
            }

        } catch (e: AcuantException) {
            Log.e("Acuant Error", e.toString())
        }
    }


    private fun showDocumentCapture(isBack: Boolean = false) {
        activity?.let {
            val cameraIntent = Intent(
                it,
                AcuantCameraActivity::class.java
            )
            cameraIntent.putExtra(
                ACUANT_EXTRA_CAMERA_OPTIONS,
                AcuantCameraOptions
                    .DocumentCameraOptionsBuilder().setBack(isBack)
                    .build()
            )
            it.startActivityForResult(cameraIntent, Constants.ACT_DOC_CAM_CODE)
        }
    }

    private fun showFaceCapture() {
        activity?.let {
            val cameraIntent = Intent(
                it,
                AcuantFaceCameraActivity::class.java
            )
            cameraIntent.putExtra(
                ACUANT_EXTRA_FACE_CAPTURE_OPTIONS,
                FaceCaptureOptions(
                    cameraMode = CameraMode.HgLiveness,
//                    showOval = true,
                    totalCaptureTime = 0,
                )
            )
            it.startActivityForResult(cameraIntent, Constants.ACT_FACE_CAM_CODE)
        }
    }


//    private var mrzCameraLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == RESULT_OK) {
//                val data: Intent? = result.data
//                val mrzResult = data?.getSerializableExtra(ACUANT_EXTRA_MRZ_RESULT) as MrzResult?
//
//                val confirmNFCDataActivity = Intent(this, NfcConfirmationActivity::class.java)
//                confirmNFCDataActivity.putExtra("DOB", mrzResult?.dob)
//                confirmNFCDataActivity.putExtra("DOE", mrzResult?.passportExpiration)
//                confirmNFCDataActivity.putExtra("DOCNUMBER", mrzResult?.passportNumber)
//                confirmNFCDataActivity.putExtra("COUNTRY", mrzResult?.country)
//                confirmNFCDataActivity.putExtra("THREELINE", mrzResult?.threeLineMrz)
//
//                this.startActivity(confirmNFCDataActivity)
//            } else if (result.resultCode == RESULT_CANCELED) {
//                Log.d(TAG, "User canceled mrz capture")
//            }
//        }


    // Flutter plugin

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        mChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "ca.couver.acuantchannel")
        mChannel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        mChannel?.setMethodCallHandler(null)
        mChannel = null
    }


    // Flutter Activity Aware

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity;
        binding.addActivityResultListener(this)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onDetachedFromActivity() {
    }
}
