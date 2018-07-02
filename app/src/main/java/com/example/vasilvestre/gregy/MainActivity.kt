package com.example.vasilvestre.gregy

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private lateinit var mButtonInputDriver: ButtonInputDriver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mButtonInputDriver = ButtonInputDriver(
                BoardDefaults.gpioForButton,
                Button.LogicState.PRESSED_WHEN_LOW,
                KeyEvent.KEYCODE_SPACE)
        mButtonInputDriver.register()

        setContentView(R.layout.activity_main)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            sample_text.text = "Capture !"
            return true
        }
        return super.onKeyUp(keyCode,event)
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

    companion object {
        private val TAG = ButtonActivity::class.java.simpleName
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
