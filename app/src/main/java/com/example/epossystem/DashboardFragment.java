package com.example.epossystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private SharedViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        viewModel = new ViewModelProvider((EposApplication) requireActivity().getApplication()).get(SharedViewModel.class);

        TextView statusText = view.findViewById(R.id.statusTextView);
        TextView usbStatusText = view.findViewById(R.id.usbStatusTextView);
        TextView deviceText = view.findViewById(R.id.deviceNameText);
        TextView totalAmountText = view.findViewById(R.id.receivedTotalAmount);
        TextView lastUpdatedText = view.findViewById(R.id.lastUpdatedText);
        MaterialButton listenButton = view.findViewById(R.id.listenButton);
        MaterialButton openErpButton = view.findViewById(R.id.openErpButton);

        viewModel.getStatus().observe(getViewLifecycleOwner(), s -> {
            statusText.setText(s);
            if (s.equals("LISTENING...")) {
                listenButton.setEnabled(false);
                listenButton.setText("Listening...");
            } else if (s.equals("CONNECTED")) {
                listenButton.setEnabled(false);
                listenButton.setText("Connected");
            } else {
                listenButton.setEnabled(true);
                listenButton.setText("Start Receiving");
            }
        });

        viewModel.getStatusColor().observe(getViewLifecycleOwner(), statusText::setTextColor);
        
        viewModel.getUsbStatus().observe(getViewLifecycleOwner(), usbStatusText::setText);
        viewModel.getUsbStatusColor().observe(getViewLifecycleOwner(), usbStatusText::setTextColor);

        viewModel.getDeviceName().observe(getViewLifecycleOwner(), deviceText::setText);
        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), amount -> 
            totalAmountText.setText(String.format(Locale.getDefault(), "৳ %.2f", amount)));
        viewModel.getLastUpdated().observe(getViewLifecycleOwner(), lastUpdatedText::setText);

        listenButton.setOnClickListener(v -> ((MainActivity) requireActivity()).onListenButtonClicked());

        openErpButton.setOnClickListener(v -> ((MainActivity) requireActivity()).openWebErp());

        return view;
    }
}