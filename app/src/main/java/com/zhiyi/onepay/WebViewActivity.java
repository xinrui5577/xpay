package com.zhiyi.onepay;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;

import com.zhiyi.onepay.components.ZyWebViewClient;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private static String LOG_TAG = "ZYKJ";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        webView = (WebView)findViewById(R.id.web_view);
        webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        webView.setWebViewClient(new ZyWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        //webView.setWebContentsDebuggingEnabled(true);
        Intent intent = getIntent();
        String Url = intent.getStringExtra(AppConst.ACTION_URL);
        if (Url == null) {
            Url = "https://www.baidu.com";
        }
        webView.loadUrl(Url);
    }

}
