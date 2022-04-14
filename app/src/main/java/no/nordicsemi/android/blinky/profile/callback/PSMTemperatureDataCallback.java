package no.nordicsemi.android.blinky.profile.callback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import kotlin.UByteArray;
import no.nordicsemi.android.ble.callback.DataSentCallback;
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

public abstract class  PSMTemperatureDataCallback implements ProfileDataCallback, DataSentCallback, PSMTemepratureCallback {


    @Override
    public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        parse(device, data);
    }

    @Override
    public void onDataSent(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        parse(device, data);
    }

    private void parse(@NonNull final BluetoothDevice device, @NonNull final Data data) {
//        if (data.size() != 1) {
//            onInvalidDataReceived(device, data);
//            return;
//        }
         final byte[] temperature = data.getValue();
        onTemperatureChanged(device,temperature );
//        final int state = data.getIntValue(Data., 0);
//        if (state == STATE_ON) {
//            onLedStateChanged(device, true);
//        } else if (state == STATE_OFF) {
//            onLedStateChanged(device, false);
//        } else {
//            onInvalidDataReceived(device, data);
//        }
    }
}
