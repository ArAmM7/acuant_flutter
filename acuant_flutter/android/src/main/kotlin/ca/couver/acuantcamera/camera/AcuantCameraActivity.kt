package ca.couver.acuantcamera.camera

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ca.couver.acuant.R
import ca.couver.acuantcamera.camera.barcode.AcuantBarcodeCameraFragment
import ca.couver.acuantcamera.camera.document.AcuantDocCameraFragment
import ca.couver.acuantcamera.camera.mrz.AcuantMrzCameraFragment
import ca.couver.acuantcamera.constant.*
import ca.couver.acuantcamera.interfaces.ICameraActivityFinish
import ca.couver.acuantcamera.constant.*
import ca.couver.acuantcamera.helper.MrzResult
import ca.couver.acuant.databinding.ActivityCameraBinding
import com.acuant.acuantcommon.model.AcuantError

class AcuantCameraActivity : AppCompatActivity(), ICameraActivityFinish {

    private lateinit var binding: ActivityCameraBinding

    companion object {
        // Static storage for captured bytes (v11.6.0+)
        private var latestCapturedBytes: ByteArray? = null
        
        /**
         * Get the latest captured image bytes
         * @param clearBytesAfterRead If true, clears the bytes after reading (recommended for security)
         */
        @JvmStatic
        fun getLatestCapturedBytes(clearBytesAfterRead: Boolean = true): ByteArray? {
            val bytes = latestCapturedBytes
            if (clearBytesAfterRead) {
                latestCapturedBytes = null
            }
            return bytes
        }
    }

    //Camera Launch
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.MaterialTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideTopMenu()

        val unserializedOptions = intent.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS)

        val options: AcuantCameraOptions = if (unserializedOptions == null) {
            AcuantCameraOptions.DocumentCameraOptionsBuilder().build()
        } else {
            unserializedOptions as AcuantCameraOptions
        }

        //start the camera if this is the first time the activity is created (camera already exists otherwise)
        if (savedInstanceState == null) {
            val cameraFragment: AcuantBaseCameraFragment = when (options.cameraMode) {
                AcuantCameraOptions.CameraMode.BarcodeOnly -> {
                    AcuantBarcodeCameraFragment.newInstance(options)
                }
                AcuantCameraOptions.CameraMode.Mrz -> {
                    AcuantMrzCameraFragment.newInstance(options)
                }
                else -> { //Document
                    AcuantDocCameraFragment.newInstance(options)
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, cameraFragment as Fragment)
                .commit()

        }
    }

    //Camera Responses
    override fun onCameraDone(imageUrl: String, barCodeString: String?) {
        // Store bytes in static field for SDK 11.6.0+
        val file = java.io.File(imageUrl)
        if (file.exists()) {
            latestCapturedBytes = file.readBytes()
        }
        
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_IMAGE_URL, imageUrl)
        intent.putExtra(ACUANT_EXTRA_PDF417_BARCODE, barCodeString)
        this@AcuantCameraActivity.setResult(RESULT_OK, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onCameraDone(mrzResult: MrzResult) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_MRZ_RESULT, mrzResult)
        this@AcuantCameraActivity.setResult(RESULT_OK, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onCameraDone(barCodeString: String) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_PDF417_BARCODE, barCodeString)
        this@AcuantCameraActivity.setResult(RESULT_OK, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onCancel() {
        val intent = Intent()
        this@AcuantCameraActivity.setResult(RESULT_CANCELED, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onError(error: AcuantError) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_ERROR, error)
        this@AcuantCameraActivity.setResult(RESULT_ERROR, intent)
        this@AcuantCameraActivity.finish()
    }

    //misc/housekeeping
    override fun onBackPressed() {
        onCancel()
    }

    private fun hideTopMenu() {
        actionBar?.hide()
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        hideTopMenu()
    }
}