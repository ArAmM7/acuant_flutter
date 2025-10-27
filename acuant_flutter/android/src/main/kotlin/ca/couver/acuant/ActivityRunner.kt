package ca.couver.acuant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ca.couver.acuantcamera.camera.AcuantCameraActivity
import ca.couver.acuantcamera.camera.AcuantCameraOptions
//import ca.couver.acuantcamera.camera.AcuantCameraActivity
//import ca.couver.acuantcamera.camera.AcuantCameraOptions

import ca.couver.acuantcamera.constant.ACUANT_EXTRA_CAMERA_OPTIONS

class ActivityRunner : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.noActionbarTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runner)

        val cameraIntent = Intent(
            this@ActivityRunner,
            AcuantCameraActivity::class.java
        )
        cameraIntent.putExtra(
            ACUANT_EXTRA_CAMERA_OPTIONS,
            AcuantCameraOptions
                .DocumentCameraOptionsBuilder()
                /*Call any other methods detailed in the AcuantCameraOptions section near the bottom of the readme*/
                .build()
        )

        try {
            docCameraLauncher.launch(cameraIntent);
        } catch (e: Exception) {
            println(e.toString())
        }
//        startActivityForResult(cameraIntent, 1)
    }

    private var docCameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                // Updated for SDK 11.6.0+: Use getLatestCapturedBytes instead of file path
                val bytes = AcuantCameraActivity.getLatestCapturedBytes(clearBytesAfterRead = true)
                // Process bytes as needed
            }
        }
}