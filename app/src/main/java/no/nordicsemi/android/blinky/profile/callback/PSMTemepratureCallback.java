package no.nordicsemi.android.blinky.profile.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

public interface PSMTemepratureCallback {
    
    void onTemperatureChanged(@NonNull final BluetoothDevice device, final byte[] temperature);
}
