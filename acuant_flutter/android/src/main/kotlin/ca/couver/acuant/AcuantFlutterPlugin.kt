package ca.couver.acuant

// Basics
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
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

// Flutter
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar


class AcuantFlutterPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.ActivityResultListener {

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            registrar.activity()?.let {
                // If a background flutter view tries to register the plugin,
                // there will be no activity from the registrar,
                // we stop the registering process immediately because the plugin requires an activity.

                val AcuantFlutterPlugin = AcuantFlutterPlugin()
                AcuantFlutterPlugin.activity = it
                AcuantFlutterPlugin.mChannel =
                    MethodChannel(registrar.messenger(), "ca.couver.acuantchannel")
                AcuantFlutterPlugin.mChannel?.setMethodCallHandler(AcuantFlutterPlugin)
                registrar.addActivityResultListener(AcuantFlutterPlugin)
            }
        }

//        const val SCANRESULT = "scan"
//        const val Request_Scan = 1
//        const val TAG = "MajascanPlugin"
    }

    private var mChannel: MethodChannel? = null
    private var mResult: Result? = null
    private var activity: Activity? = null
    private var isInitialized = false

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        mResult = result;
        when (call.method) {
            Constants.REQ_INITIALIZE -> {
                val username = call.argument<String>("username")
                val password = call.argument<String>("password")
                val subscription = call.argument<String>("subscription")
                try {
                    initializeAcuantSdk(username, password, subscription)
                } catch (e: AcuantException) {
                    result.error("100", "Acuant Error", {})
                }
            }
            Constants.REQ_DOC_CAM -> {
                showDocumentCaptureCamera()
            }
            Constants.REQ_FACE_CAM -> {
                showFaceCaptureCamera()
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == Constants.ACT_DOC_CAM_CODE && resultCode == RESULT_OK && data != null) {
            val url = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
            if (url != null) {
                AcuantImagePreparation.evaluateImage(activity!!, CroppingData(url), object :
                    EvaluateImageListener {
                    override fun onSuccess(image: AcuantImage) {
                        mResult?.success(
                            hashMapOf(
                                "RAW_BYTES" to image.rawBytes,
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
        } else if (requestCode == Constants.ACT_FACE_CAM_CODE && resultCode == RESULT_OK && data != null) {
            val url = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
            if (url != null) {
                AcuantImagePreparation.evaluateImage(activity!!, CroppingData(url), object :
                    EvaluateImageListener {
                    override fun onSuccess(image: AcuantImage) {
                        mResult?.success(
                            hashMapOf(
                                "RAW_BYTES" to image.rawBytes,
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
        } else {
            mResult?.error("0", "Cancelled", null)
        }
        return false
    }

    private val initializeCallback = object : IAcuantPackageCallback {
        override fun onInitializeSuccess() {
            isInitialized = true;
            mResult?.success(true)
        }

        override fun onInitializeFailed(error: List<AcuantError>) {
            isInitialized = false;
            mResult?.success(false)
        }
    }


    private fun initializeAcuantSdk(
        username: String?,
        password: String?,
        subscription: String?,
    ) {

        println("initializeAcuantSdk")
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
                    initializeCallback
                )
            }

        } catch (e: AcuantException) {
            Log.e("Acuant Error", e.toString())
        }
    }


    private fun showDocumentCaptureCamera() {
        activity?.let {
            val cameraIntent = Intent(
                it,
                AcuantCameraActivity::class.java
            )
            cameraIntent.putExtra(
                ACUANT_EXTRA_CAMERA_OPTIONS,
                AcuantCameraOptions
                    .DocumentCameraOptionsBuilder()
                    .build()
            )
            it.startActivityForResult(cameraIntent, Constants.ACT_DOC_CAM_CODE)
        }
    }

    private fun showFaceCaptureCamera() {
        activity?.let {
            val cameraIntent = Intent(
                it,
                AcuantFaceCameraActivity::class.java
            )
            cameraIntent.putExtra(
                ACUANT_EXTRA_FACE_CAPTURE_OPTIONS,
                FaceCaptureOptions()
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
//        TODO("Not yet implemented")
    }

    override fun onDetachedFromActivity() {
//        TODO("Not yet implemented")
    }
}
