package com.firstapp.langtranslate.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.content.ContextCompat

/**
 * Manages Bluetooth audio routing for low-latency translation
 */
class BluetoothAudioManager(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = proxy as BluetoothHeadset
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = null
            }
        }
    }

    init {
        try {
            if (hasBluetoothPermission()) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                bluetoothAdapter?.getProfileProxy(
                    context,
                    profileListener,
                    BluetoothProfile.HEADSET
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Check if Bluetooth headset is connected
     */
    fun isBluetoothHeadsetConnected(): Boolean {
        return try {
            if (!hasBluetoothPermission()) return false

            bluetoothHeadset?.connectedDevices?.isNotEmpty() ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Route audio to Bluetooth device
     */
    fun routeAudioToBluetooth(): Boolean {
        return try {
            if (isBluetoothHeadsetConnected()) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Stop Bluetooth SCO audio
     */
    fun stopBluetoothAudio() {
        try {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get connected Bluetooth device name
     */
    fun getConnectedDeviceName(): String? {
        return try {
            if (!hasBluetoothPermission()) return null

            bluetoothHeadset?.connectedDevices?.firstOrNull()?.name
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Check Bluetooth permission
     */
    private fun hasBluetoothPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
