package ca.couver.acuantfacecapture.interfaces

import com.acuant.acuantcommon.background.AcuantListener

interface IFaceCameraActivityFinish : AcuantListener{
    fun onCameraDone(imageUrl: String)
    fun onCancel()
}