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
import java.util.List;

public class HistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        SharedViewModel viewModel = new ViewModelProvider((EposApplication) requireActivity().getApplication()).get(SharedViewModel.class);
        TextView historyDataText = view.findViewById(R.id.historyDataText);

        viewModel.getHistory().observe(getViewLifecycleOwner(), history -> {
            if (history == null || history.isEmpty()) {
                historyDataText.setText("No transactions yet.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (String item : history) {
                    sb.append(item).append("\n\n");
                }
                historyDataText.setText(sb.toString());
            }
        });

        return view;
    }
}