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
import ca.couver.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL

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
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer
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
        } else {
            mResult = result;
            when (call.method) {
                Constants.REQ_INITIALIZE -> {

                    val username = call.argument<String>("username")
                    val password = call.argument<String>("password")
                    val subscription = call.argument<String>("subscription")
                    try {
                        initializeAcuantSdk(username, password, subscription)
                    } catch (e: AcuantException) {
                        result.error("100", e.localizedMessage, {})
                    }
                }
                Constants.REQ_DOC_CAM -> {

                    val isBack = call.argument<Boolean>("isBack")
                    showDocumentCapture(isBack ?: false)
                }
                Constants.REQ_FACE_CAM ->{

                     showFaceCapture()
                }
                else -> result.notImplemented()
            }
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
        val url = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
        if (url == null) {
            mResult?.error("1", "Something went wrong", "Can not find captured image")
        } else {
            AcuantImagePreparation.evaluateImage(activity!!, CroppingData(url), object :
                EvaluateImageListener {
                override fun onSuccess(image: AcuantImage) {
                    val bitmap = BitmapFactory.decodeByteArray(image.rawBytes, 0, image.rawBytes.size)
                    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, (720*image.aspectRatio).toInt(), 720, true)
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // Adjust the quality as needed
                    val compressedImageBytes = outputStream.toByteArray()
                    
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
                }

                override fun onError(error: AcuantError) {
                    mResult?.error(
                        error.errorCode.toString(),
                        error.errorDescription,
                        error.additionalDetails
                    )
                }
            })
        }
    }

    private fun handleFaceCapture(data: Intent?) {
        val url = data?.getStringExtra(ACUANT_EXTRA_FACE_IMAGE_URL)
        if (url == null) {
            mResult?.error("1", "Something went wrong", "Can not find captured image")
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
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if(resultSubmitted) {
            return false
        }
        when (resultCode) {
            RESULT_OK -> {
                when (requestCode) {
                    Constants.ACT_DOC_CAM_CODE -> handleDocumentCapture(data)
                    Constants.ACT_FACE_CAM_CODE -> handleFaceCapture(data)
                    else -> mResult?.error(
                        "2",
                        "Not implemented",
                        "It's not a result from camera intent"
                    )
                }
            }
            else -> mResult?.error(
                "3",
                "Operation cancelled",
                "The operation was cancelled or failed"
            )
        }
        resultSubmitted = true
        return true
    }

    private val handleInitialize = object : IAcuantPackageCallback {
        override fun onInitializeSuccess() {
            isInitialized = true;
            mResult?.success(true)
        }

        override fun onInitializeFailed(errors: List<AcuantError>) {
            isInitialized = false;
            var error: AcuantError? = null;
            if (errors.isNotEmpty()) {
                error = errors[0];
            }
            mResult?.error(
                error?.errorCode?.toString() ?: "9",
                error?.errorDescription ?: "Unknown error",
                null
            )
        }
    }


    private fun initializeAcuantSdk(
        username: String?,
        password: String?,
        subscription: String?,
    ) {
        try {
            if (activity == null) {
                mResult?.error("100", "Android Activity not found", null);
            } else {
                Credential.init(
                    username,
                    password,
                    subscription,
                    "https://frm.acuant.net",
                    "https://services.assureid.net",
                    "https://medicscan.acuant.net",
                    "https://us.passlive.acuant.net",
                    "https://acas.acuant.net",
                    "https://ozone.acuant.net"
                )

                AcuantInitializer.initialize(
                    null,
                    activity!!,
                    listOf(ImageProcessorInitializer(), MrzCameraInitializer()),
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
