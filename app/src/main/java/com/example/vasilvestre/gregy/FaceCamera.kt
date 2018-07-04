package com.example.vasilvestre.gregy

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import java.util.*
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.CaptureResult

public class FaceCamera private constructor() {
    private val TAG = FaceCamera::class.java.simpleName

    private val IMAGE_WIDTH = 666
    private val IMAGE_HEIGHT = 666
    private val MAX_IMAGES = 1

    private lateinit var mCameraDevice : CameraDevice

    private lateinit var mCaptureSession: CameraCaptureSession

    private lateinit var mImageReader : ImageReader

    private lateinit var mCameraManager: CameraManager

    private object Holder { val INSTANCE = FaceCamera() }

    companion object {
        val instance: FaceCamera by lazy { Holder.INSTANCE }
    }

    public fun initializeCamera(context : Context, backgroundHandler: Handler, imageAvailableListener: ImageReader.OnImageAvailableListener) {
        mCameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val camIds = mCameraManager.cameraIdList
        val id = camIds[0]
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH,IMAGE_HEIGHT, ImageFormat.JPEG, MAX_IMAGES)
        mImageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)
        mCameraManager.openCamera(id, mStateCallback, backgroundHandler)
    }

    private val mStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, i: Int) {
            cameraDevice.close()
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            mCameraDevice.close()
        }
    }

    public fun takePicture() {
        mCameraDevice.createCaptureSession(
                Collections.singletonList(mImageReader.surface),
                mSessionCallback,
                null
        )
    }

    private val mSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            mCaptureSession = cameraCaptureSession
            triggerImageCapture()
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
        }
    }

    private fun triggerImageCapture() {
        val captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder.addTarget(mImageReader.surface)
        captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null)
    }

    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {}

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) { session.close() }
    }

    fun shutDown() {
        mCameraDevice.close()
    }

}