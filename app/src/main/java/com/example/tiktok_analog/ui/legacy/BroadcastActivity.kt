package com.example.tiktok_analog.ui.legacy

import android.app.AlertDialog
import android.graphics.Color
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.tiktok_analog.R
import kotlinx.android.synthetic.main.broadcast.*
import java.io.IOException


class BroadcastActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private val REQUEST_ID_READ_WRITE_PERMISSION = 99
    private val REQUEST_ID_IMAGE_CAPTURE = 100
    private val REQUEST_ID_VIDEO_CAPTURE = 101

    private var surfaceHolder: SurfaceHolder? = null
    private var camera: Camera? = null
    private var cameraId = 0
    private var rotation = 0

    private var isBroadcastStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.broadcast)

        backArrowButton.setOnClickListener {
            onBackPressed()
        }

        cameraId = CameraInfo.CAMERA_FACING_BACK
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        surfaceHolder = surfaceView.holder
        surfaceHolder?.addCallback(this)

        flipCameraButton.setOnClickListener {
            flipCamera()
        }

        flipCameraButton2.setOnClickListener {
            flipCamera()
        }

        startBroadcastButton.setOnClickListener {
            startBroadcast()
        }

        broadcastTitle.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                startBroadcastButton.isEnabled = s.isNotBlank()

                startBroadcastButton.backgroundTintList =
                    applicationContext.resources.getColorStateList(
                        if (s.isNotBlank()) R.color.buttonEnabledBg else R.color.buttonDisabledBg
                    )
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun startBroadcast() {
        preStartBroadcast.visibility = View.GONE
        broadcastStarted.visibility = View.VISIBLE
        flipCameraButton2.visibility = View.VISIBLE
    }

    private fun flipCamera() {
        val id =
            if (cameraId == CameraInfo.CAMERA_FACING_BACK) {
                CameraInfo.CAMERA_FACING_FRONT
            } else {
                CameraInfo.CAMERA_FACING_BACK
            }
        openCamera(id)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        openCamera(CameraInfo.CAMERA_FACING_BACK)
    }

    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  You should at this point update
     * the imagery in the surface.  This method is always called at least
     * once, after [.surfaceCreated].
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width The new width of the surface.
     * @param height The new height of the surface.
     */
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
//        TODO("Not yet implemented")
    }

    /**
     * This is called immediately before a surface is being destroyed. After
     * returning from this call, you should no longer try to access this
     * surface.  If you have a rendering thread that directly accesses
     * the surface, you must ensure that thread is no longer touching the
     * Surface before returning from this function.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder?) {
//        TODO("Not yet implemented")
    }

    private fun openCamera(id: Int): Boolean {
        var result = false
        cameraId = id
        releaseCamera()
        try {
            camera = Camera.open(cameraId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (camera != null) {
            try {
                setUpCamera(camera!!)
                camera!!.setErrorCallback { _, _ -> }
                camera!!.setPreviewDisplay(surfaceHolder)
                camera!!.startPreview()
                result = true
            } catch (e: IOException) {
                e.printStackTrace()
                result = false
                releaseCamera()
            }
        }
        return result
    }

    private fun setUpCamera(c: Camera) {
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        rotation = windowManager.defaultDisplay.rotation
        var degree = 0
        when (rotation) {
            Surface.ROTATION_0 -> degree = 0
            Surface.ROTATION_90 -> degree = 90
            Surface.ROTATION_180 -> degree = 180
            Surface.ROTATION_270 -> degree = 270
            else -> {
            }
        }
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            // frontFacing
            rotation = (info.orientation + degree) % 330
            rotation = (360 - rotation) % 360
        } else {
            // Back-facing
            rotation = (info.orientation - degree + 360) % 360
        }
        c.setDisplayOrientation(rotation)
        val params: Camera.Parameters = c.parameters
//        // showFlashButton(params)
//        val focusModes: List<String> = params.supportedFlashModes
//        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
//            params.flashMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
//        }
        params.setRotation(rotation)
    }

    private fun releaseCamera() {
        try {
            if (camera != null) {
                camera!!.setPreviewCallback(null)
                camera!!.setErrorCallback(null)
                camera!!.stopPreview()
                camera!!.release()
                camera = null
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("error", e.toString())
            camera = null
        }
    }

    override fun onBackPressed() {
        if(!isBroadcastStarted) {
            super.onBackPressed()
            return
        }
        val alertDialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle("Вы уверены, что хотите прервать добавление видео?")
            .setMessage("Это приведет к удалению введенных вами данных")
            .setPositiveButton("Да, я уверен") { _, _ ->
                super.onBackPressed()
            }.setNegativeButton("Нет, остаться") { dialog, _ ->
                dialog.cancel()
            }.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
    }
}