package com.example.epossystem;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

public class EposApplication extends Application implements ViewModelStoreOwner {

    private final ViewModelStore viewModelStore = new ViewModelStore();

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }
}
