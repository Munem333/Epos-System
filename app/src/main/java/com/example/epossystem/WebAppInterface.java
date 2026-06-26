package com.example.epossystem;

import android.webkit.JavascriptInterface;

public class WebAppInterface {

    private final SharedViewModel viewModel;

    public WebAppInterface(SharedViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @JavascriptInterface
    public void sendOrder(String json) {
        viewModel.submitWebOrder(json);
    }

    @JavascriptInterface
    public void sendPlainText(String text) {
        viewModel.submitWebOrder(text);
    }

    @JavascriptInterface
    public void sendTotal(int total) {
        viewModel.submitWebOrder(String.valueOf(total));
    }
}
