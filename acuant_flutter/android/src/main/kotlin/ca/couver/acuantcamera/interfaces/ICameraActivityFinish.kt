package ca.couver.acuantcamera.interfaces

import ca.couver.acuantcamera.helper.MrzResult
import com.acuant.acuantcommon.background.AcuantListener

interface ICameraActivityFinish : AcuantListener{
    fun onCameraDone(imageUrl: String, barCodeString: String?)
    fun onCameraDone(mrzResult: MrzResult)
    fun onCameraDone(barCodeString: String)
    fun onCancel()
}