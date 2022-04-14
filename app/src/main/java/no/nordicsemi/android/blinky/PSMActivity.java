/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.ble.observer.ConnectionObserver;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.databinding.ActivityPsmBinding;

import no.nordicsemi.android.blinky.viewmodels.PSMViewModel;

public class PSMActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";

    private PSMViewModel viewModel;
    private ActivityPsmBinding binding;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPsmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final Intent intent = getIntent();
        final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        final String deviceName = device.getName();
        final String deviceAddress = device.getAddress();

        final MaterialToolbar toolbar = binding.toolbar;
        toolbar.setTitle(deviceName != null ? deviceName : getString(R.string.unknown_device));
        toolbar.setSubtitle(deviceAddress);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configure the view model.
        viewModel = new ViewModelProvider(this).get(PSMViewModel.class);
        viewModel.connect(device);

        // Set up views.

//        binding.ledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.setLedState(isChecked));
        binding.infoNotSupported.actionRetry.setOnClickListener(v -> viewModel.reconnect());
        binding.infoTimeout.actionRetry.setOnClickListener(v -> viewModel.reconnect());

        viewModel.getConnectionState().observe(this, state -> {
            switch (state) {
                case CONNECTING:
                    binding.progressContainer.setVisibility(View.VISIBLE);
                    binding.infoNotSupported.container.setVisibility(View.GONE);
                    binding.infoTimeout.container.setVisibility(View.GONE);
                    binding.connectionState.setText(R.string.state_connecting);
                    break;
                case INITIALIZING:
                    binding.connectionState.setText(R.string.state_initializing);
                    break;
                case READY:
                    binding.progressContainer.setVisibility(View.GONE);
                    binding.deviceContainer.setVisibility(View.VISIBLE);
                    onConnectionStateChanged(true);
                    break;
                case DISCONNECTING:
                    onConnectionStateChanged(false);
                    break;
            }
        });
        viewModel.getTemprature().observe(this, tmp -> {
            Log.w("temp", "got temp");
            binding.ledState.setText(tmp);
//            binding.ledSwitch.setChecked(isOn);
        });
//        viewModel.getLedState().observe(this, isOn -> {
//            binding.ledState.setText(isOn ? R.string.turn_on : R.string.turn_off);
//            binding.ledSwitch.setChecked(isOn);
//        });
//        viewModel.getButtonState().observe(this,
//                pressed -> binding.buttonState.setText(pressed ?
//                        R.string.button_pressed : R.string.button_released));
    }



    private void onConnectionStateChanged(final boolean connected) {
        binding.ledSwitch.setEnabled(connected);
        if (!connected) {
            binding.ledSwitch.setChecked(false);
//            binding.buttonState.setText(R.string.button_unknown);
        }
    }
}
