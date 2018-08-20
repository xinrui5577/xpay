package com.zhiyi.onepay;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.ClientCertRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.zhiyi.onepay.components.ZyWebViewClient;

public class WebViewActivity extends AppCompatActivity {
    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        webView = findViewById(R.id.web_view);
        webView.setWebViewClient(new ZyWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        //webView.setWebContentsDebuggingEnabled(true);
        Intent intent = getIntent();
        String Url = intent.getStringExtra(AppConst.ACTION_URL);
        if(Url==null){
            Url = "https://www.baidu.com";
        }
        webView.loadUrl(Url);
    }
}
