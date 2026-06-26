package com.example.epossystem;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class ErpWebActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erp_web);

        SharedViewModel viewModel = new ViewModelProvider((EposApplication) getApplication()).get(SharedViewModel.class);
        WebView webView = findViewById(R.id.erpWebView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(viewModel), "AndroidPOS");

        String url = getIntent().getStringExtra("ERP_URL");
        if (url == null || url.isEmpty()) {
            url = "file:///android_asset/erp/index.html";
        }
        webView.loadUrl(url);
    }
}
