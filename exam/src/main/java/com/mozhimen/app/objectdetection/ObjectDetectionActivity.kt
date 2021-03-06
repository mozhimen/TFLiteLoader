package com.mozhimen.app.objectdetection

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.os.Bundle
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.mozhimen.abilityk.cameraxk.helpers.ImageConverter
import com.mozhimen.app.R
import com.mozhimen.app.databinding.ActivityObjectDetectionBinding
import com.mozhimen.basick.basek.BaseKActivity
import com.mozhimen.basick.basek.BaseKViewModel
import com.mozhimen.basick.extsk.showToast
import com.mozhimen.basick.utilk.UtilKBitmap
import com.mozhimen.componentk.permissionk.PermissionK
import com.mozhimen.componentk.permissionk.annors.PermissionKAnnor
import com.mozhimen.componentk.statusbark.StatusBarK
import com.mozhimen.componentk.statusbark.annors.StatusBarKAnnor
import com.mozhimen.componentk.statusbark.annors.StatusBarKType
import com.mozhimen.objectdetector.TFLiteObjectDetector
import com.mozhimen.objectdetector.commons.IObjectDetectorListener
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.concurrent.locks.ReentrantLock

@PermissionKAnnor(permissions = [Manifest.permission.CAMERA])
class ObjectDetectionActivity :
    BaseKActivity<ActivityObjectDetectionBinding, BaseKViewModel>(R.layout.activity_object_detection) {

    private lateinit var _tfLiteObjectDetector: TFLiteObjectDetector
    private val _objectDetectorListener: IObjectDetectorListener = object : IObjectDetectorListener {
        override fun onError(error: String) {
            runOnUiThread {
                error.showToast()
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onResults(
            imageWidth: Int,
            imageHeight: Int,
            inferenceTime: Long,
            results: MutableList<Detection>?
        ) {
            runOnUiThread {
                results?.let {
                    vb.objectDetectionOverlay.setObjectRect(imageWidth, imageHeight, results)
                    vb.objectDetectionTxtCount.text = "??????????????????:${results.size}"
                }
            }
        }
    }

    override fun initData(savedInstanceState: Bundle?) {
        PermissionK.initPermissions(this) {
            if (it) {
                initView(savedInstanceState)
            } else {
                PermissionK.applySetting(this)
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        initLiteLoader()
        initCamera()
    }

    private fun initLiteLoader() {
        _tfLiteObjectDetector =
            TFLiteObjectDetector.create(
                "efficientdet-lite4.tflite",
                listener = _objectDetectorListener,
                resultSize = 200,
                threshold = 0.29f
            )
    }

    private fun initCamera() {
        vb.objectDetectionPreview.initCamera(this, CameraSelector.DEFAULT_BACK_CAMERA)
        vb.objectDetectionPreview.setImageAnalyzer(_frameAnalyzer)
        vb.objectDetectionPreview.startCamera()
    }

    private val _frameAnalyzer: ImageAnalysis.Analyzer by lazy {
        object : ImageAnalysis.Analyzer {
            private val _reentrantLock = ReentrantLock()

            @SuppressLint("UnsafeOptInUsageError", "SetTextI18n")
            override fun analyze(image: ImageProxy) {
                try {
                    _reentrantLock.lock()
                    val bitmap: Bitmap = ImageConverter.yuv2Bitmap(image)!!

                    _tfLiteObjectDetector.detect(bitmap, image.imageInfo.rotationDegrees)
                } finally {
                    _reentrantLock.unlock()
                }

                image.close()
            }
        }
    }
}