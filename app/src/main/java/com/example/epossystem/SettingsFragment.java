package com.example.epossystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        SharedViewModel viewModel = new ViewModelProvider((EposApplication) requireActivity().getApplication()).get(SharedViewModel.class);
        MaterialButton clearBtn = view.findViewById(R.id.clearHistoryBtn);

        clearBtn.setOnClickListener(v -> viewModel.clearHistory());

        return view;
    }
}