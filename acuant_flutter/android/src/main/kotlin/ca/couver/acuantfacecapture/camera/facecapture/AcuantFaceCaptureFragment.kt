package ca.couver.acuantfacecapture.camera.facecapture

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.camera.core.ImageAnalysis
import com.acuant.acuantcommon.model.AcuantError
import com.acuant.acuantcommon.model.ErrorCodes
import com.acuant.acuantcommon.model.ErrorDescriptions
import ca.couver.acuantfacecapture.camera.AcuantBaseFaceCameraFragment
import ca.couver.acuant.databinding.FaceFragmentUiBinding
import ca.couver.acuantfacecapture.detector.FaceFrameAnalyzer
import ca.couver.acuantfacecapture.detector.FaceState
import ca.couver.acuantfacecapture.helper.RectHelper
import ca.couver.acuantfacecapture.interfaces.IAcuantSavedImage
import ca.couver.acuantfacecapture.model.FaceCaptureOptions
import ca.couver.acuantfacecapture.overlays.FacialGraphic
import ca.couver.acuantfacecapture.overlays.FacialGraphicOverlay
import kotlin.math.ceil

enum class FaceCameraState {
    Align, MoveCloser, MoveBack, FixRotation, KeepSteady, Hold, Blink, Capturing
}

class AcuantFaceCaptureFragment : AcuantBaseFaceCameraFragment() {

    private var cameraUiContainerBinding: FaceFragmentUiBinding? = null
    private var mFacialGraphicOverlay: FacialGraphicOverlay? = null
    private var mFacialGraphic: FacialGraphic? = null
    private var faceImage: ImageView? = null
    private var lastFacePosition: Rect? = null
    private var startTime = System.currentTimeMillis()
    private var requireBlink = false
    private var userHasBlinked = false
    private var userHasHadOpenEyes = false
    private var lastState: FaceState = FaceState.NoFace

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraUiContainerBinding?.root?.let {
            fragmentCameraBinding!!.uiHolder.removeView(it)
        }

        cameraUiContainerBinding = FaceFragmentUiBinding.inflate(
            LayoutInflater.from(requireContext()),
            fragmentCameraBinding!!.uiHolder,
            true
        )

        mFacialGraphicOverlay = cameraUiContainerBinding?.faceOverlay
        faceImage = cameraUiContainerBinding?.blankFaceImage
        faceImage?.imageAlpha = 153

        mFacialGraphicOverlay?.setOptions(acuantOptions)
    }

    override fun onResume() {
        super.onResume()
        val facialGraphicOverlay = mFacialGraphicOverlay
        if (facialGraphicOverlay != null) {
            if (mFacialGraphic == null) {
                val facialGraphic = FacialGraphic(facialGraphicOverlay)
                facialGraphic.setOptions(acuantOptions)
                mFacialGraphic = facialGraphic
            }
            facialGraphicOverlay.add(mFacialGraphic!!)
        }
    }

    override fun onDestroy() {
        mFacialGraphicOverlay?.clear()
        mFacialGraphicOverlay = null
        mFacialGraphic = null
        super.onDestroy()
    }

    override fun resetWorkflow() {
        resetTimer()
        mFacialGraphicOverlay?.clear()
        mFacialGraphic?.updateLiveFaceDetails(null, FaceCameraState.Align)
    }

    fun changeToHGLiveness() {
        requireBlink = true
    }

    private fun resetTimer(resetBlinkState: Boolean = true) {
        if (resetBlinkState) {
            userHasBlinked = false
            userHasHadOpenEyes = false
        }
        startTime = System.currentTimeMillis()
    }

    private fun onFaceDetected(rect: Rect?, state: FaceState) {
        if (capturing || !isAdded)
            return
        val analyzerSize = imageAnalyzer?.resolutionInfo?.resolution
        val previewView = fragmentCameraBinding?.viewFinder
        val boundingBox = if (rect != null && analyzerSize != null && previewView != null) {
            mFacialGraphic?.setWidth(previewView.width)
            RectHelper.scaleRect(rect, analyzerSize, previewView)
        } else {
            null
        }
        val realState = if (previewView != null && boundingBox != null) {
            val view =
                Rect(previewView.left, previewView.top, previewView.right, previewView.bottom)
            if (!view.contains(boundingBox)) {
                FaceState.NoFace
            } else {
                state
            }
        } else {
            state
        }
        faceImage?.visibility = View.INVISIBLE
        when (realState) {
            FaceState.NoFace -> {
                faceImage?.visibility = View.VISIBLE
                mFacialGraphicOverlay?.setState(FaceCameraState.Align)
                hideBlink()
                mFacialGraphic?.updateLiveFaceDetails(null, FaceCameraState.Align)
                resetTimer()
            }
            FaceState.FaceTooFar -> {
                mFacialGraphicOverlay?.setState(FaceCameraState.MoveCloser)
                hideBlink()
                mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.MoveCloser)
                resetTimer()
            }
            FaceState.FaceTooClose -> {
                mFacialGraphicOverlay?.setState(FaceCameraState.MoveBack)
                hideBlink()
                mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.MoveBack)
                resetTimer()
            }
            FaceState.FaceAngled -> {
                mFacialGraphicOverlay?.setState(FaceCameraState.FixRotation)
                hideBlink()
                mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.FixRotation)
                resetTimer()
            }
            else -> { //good face or closed eyes
                when {
                    didFaceMove(boundingBox, lastFacePosition) -> {
                        mFacialGraphicOverlay?.setState(FaceCameraState.KeepSteady)
                        hideBlink()
                        mFacialGraphic?.updateLiveFaceDetails(
                            boundingBox,
                            FaceCameraState.KeepSteady
                        )
                        resetTimer()
                    }
                    requireBlink && !userHasBlinked -> {
                        if (userHasHadOpenEyes && lastState == FaceState.EyesClosed && realState == FaceState.GoodFace) {
                            userHasBlinked = true
                        }
                        if (realState == FaceState.GoodFace) {
                            userHasHadOpenEyes = true
                        }
                        mFacialGraphicOverlay?.setState(FaceCameraState.Blink)
                        showBlink()
                        mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.Blink)
                        resetTimer(resetBlinkState = false)
                    }
                    System.currentTimeMillis() - startTime < acuantOptions.totalCaptureTime * 1000 -> {
                        mFacialGraphicOverlay?.setState(
                            FaceCameraState.Hold,
                            acuantOptions.totalCaptureTime - ceil(((System.currentTimeMillis() - startTime) / 1000).toDouble()).toInt()
                        )
                        hideBlink()
                        mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.Hold)
                    }
                    else -> {
                        mFacialGraphicOverlay?.setState(FaceCameraState.Capturing)
                        hideBlink()
                        mFacialGraphic?.updateLiveFaceDetails(
                            boundingBox,
                            FaceCameraState.Capturing
                        )
                        if (!capturing) {
                            captureImage(object : IAcuantSavedImage {
                                override fun onSaved(uri: String) {
                                    cameraActivityListener.onCameraDone(uri)
                                }

                                override fun onError(error: AcuantError) {
                                    cameraActivityListener.onError(error)
                                }
                            })
                        }
                    }
                }
            }
        }
        lastState = realState
        lastFacePosition = boundingBox
    }

    override fun buildImageAnalyzer(screenAspectRatio: Int, rotation: Int) {
        val frameAnalyzer = try {
            FaceFrameAnalyzer { boundingBox, state ->
                onFaceDetected(boundingBox, state)
            }
        } catch (e: IllegalStateException) {
            cameraActivityListener.onError(
                AcuantError(
                    ErrorCodes.ERROR_UnexpectedError,
                    ErrorDescriptions.ERROR_DESC_UnexpectedError,
                    e.toString()
                )
            )
            return
        }

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, frameAnalyzer)
            }
    }

    companion object {
        private const val MOVEMENT_THRESHOLD = 22

        private fun didFaceMove(facePosition: Rect?, lastFacePosition: Rect?): Boolean {
            if (facePosition == null || lastFacePosition == null)
                return false
            return RectHelper.distance(facePosition, lastFacePosition) > MOVEMENT_THRESHOLD
        }


        @JvmStatic
        fun newInstance(acuantOptions: FaceCaptureOptions): AcuantFaceCaptureFragment {
            val frag = AcuantFaceCaptureFragment()
            val args = Bundle()
            args.putSerializable(INTERNAL_OPTIONS, acuantOptions)
            frag.arguments = args
            return frag
        }
    }
}