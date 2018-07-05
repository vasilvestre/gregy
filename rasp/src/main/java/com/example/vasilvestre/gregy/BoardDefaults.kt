package com.example.vasilvestre.gregy

import android.os.Build

@SuppressWarnings("WeakerAccess")
object BoardDefaults {
    private const val DEVICE_RPI3 = "rpi3"
    private const val DEVICE_IMX6UL_PICO = "imx6ul_pico"
    private const val DEVICE_IMX7D_PICO = "imx7d_pico"

    /**
     * Return the GPIO pin that the LED is connected on.
     */
    val gpioForLED = when (Build.DEVICE) {
        DEVICE_RPI3 -> "BCM6"
        DEVICE_IMX6UL_PICO -> "GPIO4_IO22"
        DEVICE_IMX7D_PICO -> "GPIO2_IO02"
        else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
    }

    /**
     * Return the GPIO pin that the Button is connected on.
     */
    val gpioForButton = when (Build.DEVICE) {
        DEVICE_RPI3 -> "BCM21"
        DEVICE_IMX6UL_PICO -> "GPIO2_IO03"
        DEVICE_IMX7D_PICO -> "GPIO6_IO14"
        else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
    }

    val gpioForSpeaker = when (Build.DEVICE) {
            DEVICE_RPI3 -> "PWM1"
            DEVICE_IMX6UL_PICO -> "PWM8"
            DEVICE_IMX7D_PICO -> "PWM2"
        else -> throw UnsupportedOperationException("Unknown device: " + Build.DEVICE)
    }
}