package com.example.epossystem;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.util.Set;

public class DevicesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);
        
        TextView devicesListText = view.findViewById(R.id.pairedDevicesList);
        MaterialButton scanButton = view.findViewById(R.id.scanButton);

        updatePairedDevices(devicesListText);

        scanButton.setOnClickListener(v -> updatePairedDevices(devicesListText));

        return view;
    }

    private void updatePairedDevices(TextView textView) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            textView.setText("Bluetooth permissions not granted.");
            return;
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (BluetoothDevice device : pairedDevices) {
                sb.append(device.getName()).append("\n").append(device.getAddress()).append("\n\n");
            }
            textView.setText(sb.toString());
        } else {
            textView.setText("No paired devices found.");
        }
    }
}