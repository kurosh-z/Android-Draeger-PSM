package no.nordicsemi.android.blinky.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;

import no.nordicsemi.android.blinky.profile.PSMmanager;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class PSMViewModel extends AndroidViewModel {
//    private final PSMManager psmManager;
    private final PSMmanager psmManager;

    private BluetoothDevice device;
    @Nullable
    private ConnectRequest connectRequest;

    public PSMViewModel(@NonNull final Application application) {
        super(application);

        // Initialize the manager.
        psmManager = new PSMmanager(getApplication());
    }

    public MutableLiveData<PSMmanager.State> getConnectionState() {
        return psmManager.getConnectionState();
    }


    public LiveData<String> getTemprature() {
        return psmManager.getTempereture();
    }





    /**
     * Connect to the given peripheral.
     *
     * @param target the target device.
     */
    public void connect(@NonNull final DiscoveredBluetoothDevice target) {
        // Prevent from calling again when called again (screen orientation changed).
        if (device == null) {
            device = target.getDevice();
            final LogSession logSession = Logger
                    .newSession(getApplication(), null, target.getAddress(), target.getName());
//            psmManager.setLogger(logSession);
//            reconnect();
            psmManager.connect(device.getAddress());
        }
    }

    /**
     * Reconnects to previously connected device.
     * If this device was not supported, its services were cleared on disconnection, so
     * reconnection may help.
     */
    public void reconnect() {
        if (device != null) {
            psmManager.connect(device.getAddress());
//            connectRequest = psmManager.connect(device)
//                    .retry(3, 100)
//                    .useAutoConnect(false)
//                    .then(d -> connectRequest = null);
//            connectRequest.enqueue();
        }
    }

    /**
     * Disconnect from peripheral.
     */
    private void disconnect() {
        psmManager.disconnect();
        device = null;

//        if (connectRequest != null) {
//            connectRequest.cancelPendingConnection();
//        } else if (psmManager.isConnected()) {
//            psmManager.disconnect().enqueue();
//        }
    }




    @Override
    protected void onCleared() {
        super.onCleared();
        disconnect();
    }
}
