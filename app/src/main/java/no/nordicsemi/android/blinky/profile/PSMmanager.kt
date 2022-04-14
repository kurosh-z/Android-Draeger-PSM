package no.nordicsemi.android.blinky.profile

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.util.*
import kotlin.math.pow





class PSMmanager(private val context: Context) {
    var device: BluetoothDevice? = null
    private val UUID_TEMPERATURE_SERVICE = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
    private val UUID_TEMPERATURE_CHAR = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb")
    private val UUID_TEMPERATURE_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    enum class State {
        CONNECTING, INITIALIZING, READY, DISCONNECTING, DISCONNECTED
    }

    val connectionState: MutableLiveData<State> = MutableLiveData()
    val tempereture: MutableLiveData<String> = MutableLiveData()
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothGatt: BluetoothGatt? = null

    init {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
            }
        }
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")

        }

    }


    fun connect(deviceAddress: String?): Boolean {
        if (mBluetoothAdapter == null || deviceAddress == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (device != null) {
            if ( device!!.address == deviceAddress && mBluetoothGatt != null) {
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
                return if (mBluetoothGatt!!.connect()) {
                    connectionState.postValue(State.CONNECTING)
                    true
                } else {
                    false
                }
            }
        }
        device = mBluetoothAdapter!!.getRemoteDevice(deviceAddress)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device!!.connectGatt(context, false, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        connectionState.postValue(State.CONNECTING)
        return true
    }

    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        connectionState.postValue(State.DISCONNECTING)
        mBluetoothGatt!!.disconnect()
        connectionState.postValue(State.DISCONNECTED)
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }


    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic?,
        enabled: Boolean
    ) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        // This is specific to Heart Rate Measurement.
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);
//        }
    }

    private fun updateTemperature(characteristic: BluetoothGattCharacteristic) {


        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml

        // For all other profiles, writes the data formatted in HEX.
        val data = characteristic.value
        if (data != null && data.isNotEmpty()) {
            val stringBuilder = StringBuilder(data.size)
           val exponent = data[4].toInt()
            val mantissa= data[1].toInt() + (data[2].toInt() shl 8) + (data[3].toInt() shl 16)
//            val tt= Integer.toBinaryString(0x68)
//            var value: Double = 0.0
//            for (i in 0..tt.length){
//                value+= tt[i].digitToInt() * (2).toDouble().pow(i)
//            }
//          (data[1].toInt() and 0xFF shl 16) +
//          (data[2].toInt() and 0xFF shl 8) +
//                  (data[3].toInt() )

            val numericalValue = mantissa * 10.toFloat().pow(exponent)



            tempereture.postValue(
                """
                $numericalValue
                $stringBuilder
                """.trimIndent()
            )
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {


        fun BluetoothGattCharacteristic.isReadable(): Boolean=
             containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

        fun BluetoothGattCharacteristic.isWritable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

        fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

        fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

        fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

        fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
            return properties and property != 0
        }

        fun enableNotifications(gatt:BluetoothGatt ,characteristic: BluetoothGattCharacteristic) {
            val payload =when{
                characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else ->{
                    Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                    return
                }
            }

            characteristic.getDescriptor(UUID_TEMPERATURE_DESCRIPTOR)?.let { cccDescriptor ->
                if (gatt.setCharacteristicNotification(characteristic, true) == false) {
                    Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                    return
                }
                cccDescriptor.value = payload
                val res= gatt.writeDescriptor(cccDescriptor)
                if(!res){
                    Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                    return
                }
            } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
        }






        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState.postValue(State.INITIALIZING)
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Handler(Looper.getMainLooper()).post {
                    val res = gatt.discoverServices()
                    Log.i(
                        TAG, "Attempting to start service discovery:" +
                                res
                    )
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState.postValue(State.DISCONNECTED)
                Log.i(TAG, "Disconnected from GATT server.")
            }
        }
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.w(TAG,"ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "GATT SERVICE SUCCEEDED: $status")
                val tempService = gatt.getService(UUID_TEMPERATURE_SERVICE)
                val tempChar = tempService.getCharacteristic(UUID_TEMPERATURE_CHAR)
//                if(tempChar.isReadable()){
//                    gatt.readCharacteristic(tempChar)
//                }
                enableNotifications(gatt, tempChar)
                connectionState.postValue(State.READY)
                //                readCharacteristic(tempChar);
//                boolean readTempStarted = gatt.readCharacteristic(tempChar);
//                Log.w(TAG, "reading temp service: " + readTempStarted);
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }





        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateTemperature(characteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristic?.let { updateTemperature(it) }
                ?: Log.w(TAG, "characteristic is null!")
        }
    }

    companion object {
        private val TAG = PSMmanager::class.java.simpleName
    }


}