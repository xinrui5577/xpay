package com.zhiyi.onepay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.decode.BitmapDecoder;
import com.qcloud.image.ImageClient;
import com.qcloud.image.request.GeneralOcrRequest;
import com.zhiyi.onepay.components.ZyWebViewClient;
import com.zhiyi.onepay.data.QrCodeData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewActivity extends AppCompatActivity {
    private final int PICK_CODE = 2;

    private WebView webView;
    private static String LOG_TAG = "ZYKJ";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        webView = findViewById(R.id.web_view);
        webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
