package ca.couver.acuantfacecapture.interfaces

import com.acuant.acuantcommon.background.AcuantListener

interface IAcuantSavedImage: AcuantListener {
    fun onSaved(uri: String)
}