package ca.couver.acuantcamera.overlay

import android.content.Context
import android.util.AttributeSet
import ca.couver.acuantcamera.camera.mrz.MrzCameraState
import ca.couver.acuantcamera.overlay.BaseRectangleView

class MrzRectangleView(context: Context, attr: AttributeSet?) : BaseRectangleView(context, attr) {

    fun setViewFromState(state: MrzCameraState) {
        when (state) {
            MrzCameraState.Align -> {
                setDrawBox(false)
                animateTarget = false
                paintBracket.color = paintColorBracketAlign
                paint.color = paintColorAlign
            }
            MrzCameraState.Trying -> {
                setDrawBox(true)
                animateTarget = true
                paintBracket.color = paintColorBracketHold
                paint.color = paintColorHold
            }
            MrzCameraState.Capturing -> {
                setDrawBox(true)
                animateTarget = true
                paintBracket.color = paintColorBracketCapturing
                paint.color = paintColorCapturing
            }
            else -> { //Move Closer or Reposition
                setDrawBox(true)
                animateTarget = true
                paintBracket.color = paintColorBracketCloser
                paint.color = paintColorCloser
            }
        }
        visibility = VISIBLE
    }
}