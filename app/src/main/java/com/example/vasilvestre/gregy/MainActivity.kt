package com.example.vasilvestre.gregy

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.graphics.*
import android.media.Image
import android.media.ImageReader
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import kotlinx.android.synthetic.main.activity_main.*
import android.media.ImageReader.OnImageAvailableListener
import android.os.*
import android.support.v7.recyclerview.R.attr.layoutManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.FaceServiceClient
import android.os.AsyncTask.execute
import android.view.View
import java.io.*
import com.google.android.things.contrib.driver.pwmspeaker.Speaker
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.makeCall
import org.jetbrains.anko.uiThread
import java.util.*

class MainActivity : Activity() {

    private lateinit var mButtonInputDriver: ButtonInputDriver

    private lateinit var mCamera: FaceCamera

    private var mCameraHandler: Handler? = null

    private var mCameraThread: HandlerThread? = null

    private val faceServiceClient = FaceServiceRestClient(
            "https://westcentralus.api.cognitive.microsoft.com/face/v1.0",
            "8d3742e0955b4d5fae10092d3a8ee064")

    private lateinit var mSpeaker: Speaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initGpio()

        mCameraThread = HandlerThread("CameraBackground")
        mCameraThread!!.start()
        Log.i(TAG, "Mon thread est ouvert : " + mCameraThread!!.isAlive.toString())
        mCameraHandler = Handler(mCameraThread!!.looper)

        mCamera = FaceCamera.instance
        mCamera.initializeCamera(this, mCameraHandler!!, mOnImageAvailableListener)
    }

    private val mOnImageAvailableListener = OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        // get image bytes
        val imageBuf = image.planes[0].buffer
        val imageBytes = ByteArray(imageBuf.remaining())
        imageBuf.get(imageBytes)

        val bitmapImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, null)
        val output = FileOutputStream(File(Environment.getExternalStorageDirectory(), "/tmp.png"))
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, output)

        image.close()

        onPictureTaken(bitmapImage, imageBytes)
    }


    private fun onPictureTaken(bitmapImage: Bitmap, imageBytes: ByteArray?) {
        if (imageBytes != null) {
            val outputStream = ByteArrayOutputStream()
            val inputStream = ByteArrayInputStream(imageBytes)
            val detectTask = @SuppressLint("StaticFieldLeak")
            object : AsyncTask<InputStream, String, Array<Face>>() {
                override fun doInBackground(vararg params: InputStream): Array<Face>? {
                    try {
                        publishProgress("Detecting...")
                        val result = faceServiceClient.detect(
                                params[0],
                                true,
                                true,
                                null
                        )
                        if (result == null) {
                            publishProgress("Detection Finished. Nothing detected")
                            return null
                        }
                        publishProgress(
                                String.format("Detection Finished. %d face(s) detected",
                                        result.size))
                        return result
                    } catch (e: Exception) {
                        publishProgress("Detection failed")
                        return null
                    }

                }

                override fun onPreExecute() {
                    this@MainActivity.runOnUiThread(Runnable() {
                        progressBar.visibility = View.VISIBLE
                    })
                }

                override fun onPostExecute(result: Array<Face>) {
                    this@MainActivity.runOnUiThread(Runnable() {
                        progressBar.visibility = View.GONE
                        imageView3.setImageBitmap(drawFaceRectanglesOnBitmap(bitmapImage, result))
                    })
                }
            }
            detectTask.execute(inputStream)
        }
    }

    fun changename(name: String) {
        this@MainActivity.runOnUiThread(Runnable() {
            nameView.text = name
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            sample_text.text = "Capture !"
            mCamera.takePicture()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            sample_text.text = "Appuyez pour capturer !"
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onStop() {
        mButtonInputDriver.unregister()
        mButtonInputDriver.close()

        super.onStop()
    }

    private fun initGpio() {
        mButtonInputDriver = ButtonInputDriver(
                BoardDefaults.gpioForButton,
                Button.LogicState.PRESSED_WHEN_LOW,
                KeyEvent.KEYCODE_SPACE)
        mButtonInputDriver.register()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    private fun drawFaceRectanglesOnBitmap(originalBitmap: Bitmap, faces: Array<Face>?): Bitmap {
        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        val stokeWidth = 2
        paint.strokeWidth = stokeWidth.toFloat()
        var name: String = ""
        val faceIds : ArrayList<UUID> = ArrayList()
        if (faces != null) {
            for (face in faces) {
                val faceRectangle = face.faceRectangle
                canvas.drawRect(
                        faceRectangle.left.toFloat(),
                        faceRectangle.top.toFloat(),
                        (faceRectangle.left + faceRectangle.width).toFloat(),
                        (faceRectangle.top + faceRectangle.height).toFloat(),
                        paint)
                faceIds.add(face.faceId)
                doAsync {
                    val results = faceServiceClient.identity("allowed",faceIds.toTypedArray(),10)
                    name = faceServiceClient.getPerson("allowed",results[0].candidates[0].personId).name//todo faut choisir le meilleur canddat pas tous
                    uiThread {
                        val faceRectangle = face.faceRectangle
                        canvas.drawRect(
                                faceRectangle.left.toFloat(),
                                faceRectangle.top.toFloat(),
                                (faceRectangle.left + faceRectangle.width).toFloat(),
                                (faceRectangle.top + faceRectangle.height).toFloat(),
                                paint)
                        canvas.drawText(
                                name,
                                (faceRectangle.left.toFloat()+(faceRectangle.left + faceRectangle.width).toFloat())/2,
                                (faceRectangle.top.toFloat()+faceRectangle.top + faceRectangle.height) /2,
                                paint)
                        imageView3.setImageBitmap(bitmap)
                        //bitmap.recycle()
                    }
                }
            }
        }
        return bitmap
    }
}
