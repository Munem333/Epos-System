package com.example.epossystem;

import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {

    private static final String NAME = "EposSystem";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_BT = 2;
    private static final String ACTION_USB_PERMISSION = "com.example.epossystem.USB_PERMISSION";

    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private SharedViewModel viewModel;

    // USB variables
    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager usbIoManager;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            connectUsb(device);
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (usbSerialPort != null && usbSerialPort.getDriver().getDevice().equals(device)) {
                    disconnectUsb();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider((EposApplication) getApplication()).get(SharedViewModel.class);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        
        findViewById(R.id.settingsBtn).setOnClickListener(v -> {
            bottomNav.setSelectedItemId(R.id.navigation_settings);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.navigation_history) {
                selectedFragment = new HistoryFragment();
            } else if (itemId == R.id.navigation_devices) {
                selectedFragment = new DevicesFragment();
            } else if (itemId == R.id.navigation_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }

        viewModel.getPendingWebOrder().observe(this, order -> {
            if (order == null || order.isEmpty()) {
                return;
            }
            receiveOrderFromWeb(order);
            viewModel.consumeWebOrder();
        });

        // USB setup
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, filter);
        }
        
        // Initial USB check
        checkUsbDevices();
    }

    public void onListenButtonClicked() {
        if (checkPermissions()) {
            startBluetoothServer();
        } else {
            requestPermissions();
        }
    }

    public void onUsbListenClicked() {
        checkUsbDevices();
    }

    public void openWebErp() {
        startActivity(new Intent(this, ErpWebActivity.class));
    }

    public void receiveOrderFromWeb(String message) {
        String amount = extractOrderAmount(message);
        if (amount == null || amount.isEmpty()) {
            processIncomingData(message);
            return;
        }
        processIncomingData(amount);
    }

    private String extractOrderAmount(String message) {
        if (message == null) {
            return "";
        }

        String trimmed = message.trim();

        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(trimmed);
                if (json.has("total")) {
                    return String.valueOf(json.getInt("total"));
                }
            } catch (Exception ignored) {
            }
        }

        if (trimmed.startsWith("ERP_POS|")) {
            String[] parts = trimmed.split("\\|");
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                return parts[1];
            }
        }

        return trimmed;
    }

    private void checkUsbDevices() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            viewModel.setUsbStatus("NO USB DEVICE");
            viewModel.setUsbStatusColor(ContextCompat.getColor(this, R.color.danger_red));
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        if (!usbManager.hasPermission(device)) {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), flags);
            usbManager.requestPermission(device, permissionIntent);
        } else {
            connectUsb(device);
        }
    }

    private void connectUsb(UsbDevice device) {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) return;

        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) return;

        usbSerialPort = driver.getPorts().get(0);
        try {
            usbSerialPort.open(connection);
            usbSerialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            
            usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
            usbIoManager.start();
            
            viewModel.setUsbStatus("USB CONNECTED");
            viewModel.setUsbStatusColor(ContextCompat.getColor(this, R.color.success_green));
            Toast.makeText(this, "USB Connected", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            viewModel.setUsbStatus("USB ERROR");
            e.printStackTrace();
        }
    }

    private void disconnectUsb() {
        if (usbIoManager != null) {
            usbIoManager.stop();
            usbIoManager = null;
        }
        try {
            if (usbSerialPort != null) {
                usbSerialPort.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        usbSerialPort = null;
        viewModel.setUsbStatus("USB DISCONNECTED");
        viewModel.setUsbStatusColor(ContextCompat.getColor(this, R.color.danger_red));
    }

    @Override
    public void onNewData(byte[] data) {
        String message = new String(data);
        runOnUiThread(() -> processIncomingData(message));
    }

    @Override
    public void onRunError(Exception e) {
        runOnUiThread(() -> {
            viewModel.setUsbStatus("USB ERROR");
            viewModel.setUsbStatusColor(ContextCompat.getColor(MainActivity.this, R.color.danger_red));
        });
    }

    // Existing Bluetooth methods...
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            }, REQUEST_PERMISSION_BT);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSION_BT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBluetoothServer();
            } else {
                Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startBluetoothServer() {
        if (bluetoothAdapter == null) return;
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (acceptThread != null) {
                acceptThread.cancel();
            }
            acceptThread = new AcceptThread();
            acceptThread.start();
            viewModel.setStatus("LISTENING...");
            viewModel.setStatusColor(ContextCompat.getColor(this, R.color.accent_purple));
            viewModel.setDeviceName("Waiting for connection");
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                if (mmServerSocket != null) mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        String deviceName = "Unknown Device";
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                deviceName = socket.getRemoteDevice().getName();
            }
        } catch (Exception e) {}
        
        viewModel.setStatus("CONNECTED");
        viewModel.setStatusColor(ContextCompat.getColor(this, R.color.success_green));
        viewModel.setDeviceName("Device: " + deviceName);
        
        new Thread(() -> {
            InputStream inputStream;
            try {
                inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes;

                while (true) {
                    try {
                        bytes = inputStream.read(buffer);
                        String message = new String(buffer, 0, bytes);
                        runOnUiThread(() -> processIncomingData(message));
                    } catch (IOException e) {
                        viewModel.setStatus("CONNECTION LOST");
                        viewModel.setStatusColor(ContextCompat.getColor(MainActivity.this, R.color.danger_red));
                        viewModel.setDeviceName("Device disconnected");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processIncomingData(String message) {
        String cleanMessage = message.replaceAll("[^0-9.]", "");
        String time = new SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(new Date());
        
        if (!cleanMessage.isEmpty()) {
            try {
                double amount = Double.parseDouble(cleanMessage);
                Double currentTotal = viewModel.getTotalAmount().getValue();
                viewModel.setTotalAmount((currentTotal != null ? currentTotal : 0.0) + amount);
                viewModel.setLastUpdated(new SimpleDateFormat("dd MMM yyyy • HH:mm:ss a", Locale.getDefault()).format(new Date()));
                viewModel.addHistoryItem("Received: ৳ " + String.format(Locale.getDefault(), "%.2f", amount) + " at " + time);
            } catch (Exception e) {
                viewModel.addHistoryItem("Raw Data: " + message + " at " + time);
            }
        } else {
            viewModel.addHistoryItem("Raw Data: " + message + " at " + time);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
        if (acceptThread != null) {
            acceptThread.cancel();
        }
        disconnectUsb();
    }
}
