package com.example.epossystem;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<String> status = new MutableLiveData<>("NOT CONNECTED");
    private final MutableLiveData<String> usbStatus = new MutableLiveData<>("USB DISCONNECTED");
    private final MutableLiveData<Integer> statusColor = new MutableLiveData<>(0xFFE53935); // danger_red
    private final MutableLiveData<Integer> usbStatusColor = new MutableLiveData<>(0xFFE53935);
    private final MutableLiveData<String> deviceName = new MutableLiveData<>("No Device Linked");
    private final MutableLiveData<Double> totalAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<String> lastUpdated = new MutableLiveData<>("Never");
    private final MutableLiveData<List<String>> history = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> pendingWebOrder = new MutableLiveData<>();

    public LiveData<String> getStatus() { return status; }
    public void setStatus(String value) { status.postValue(value); }

    public LiveData<String> getUsbStatus() { return usbStatus; }
    public void setUsbStatus(String value) { usbStatus.postValue(value); }

    public LiveData<Integer> getStatusColor() { return statusColor; }
    public void setStatusColor(int value) { statusColor.postValue(value); }

    public LiveData<Integer> getUsbStatusColor() { return usbStatusColor; }
    public void setUsbStatusColor(int value) { usbStatusColor.postValue(value); }

    public LiveData<String> getDeviceName() { return deviceName; }
    public void setDeviceName(String value) { deviceName.postValue(value); }

    public LiveData<Double> getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double value) { totalAmount.postValue(value); }

    public LiveData<String> getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String value) { lastUpdated.postValue(value); }

    public LiveData<List<String>> getHistory() { return history; }
    public void addHistoryItem(String item) {
        List<String> current = history.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(0, item);
        history.postValue(current);
    }
    public void clearHistory() {
        history.postValue(new ArrayList<>());
        totalAmount.postValue(0.0);
        lastUpdated.postValue("Never");
    }

    public LiveData<String> getPendingWebOrder() { return pendingWebOrder; }

    public void submitWebOrder(String orderData) {
        pendingWebOrder.postValue(orderData);
    }

    public void consumeWebOrder() {
        pendingWebOrder.setValue(null);
    }
}