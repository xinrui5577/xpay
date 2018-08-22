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
    private String currentPhotoString;
    private static String LOG_TAG = "ZYKJ";


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
        if (Url == null) {
            Url = "https://www.baidu.com";
        }
        webView.loadUrl(Url);

        readImg();

    }

    @JavascriptInterface
    public void readImg() {
        Intent pic = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pic, PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PICK_CODE) {
            if (intent != null) {
                Uri uri = intent.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                currentPhotoString = cursor.getString(idx);
                cursor.close();
                Bitmap bitmap = resizePhono();
                if (bitmap == null) {
                    Log.w(LOG_TAG, "bitmap is null" + currentPhotoString);
                    String[] mPermissionList = new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(this, mPermissionList, 100);
                    return;
                }
                BitmapDecoder decoder = new BitmapDecoder(this);
                Result rs = decoder.getRawResult(bitmap);
                if (rs != null) {
                    String txt = rs.getText();
                    Log.w(LOG_TAG, txt);
                    if (txt != null) {
                        int type;
                        if (txt.toUpperCase().startsWith("WXP://")) {
                            type = QrCodeData.TYPE_WX;
                        } else if (txt.toUpperCase().startsWith("HTTPS://QR.ALIPAY.COM")) {
                            type = QrCodeData.TYPE_ALI;
                        } else {
                            Log.w(LOG_TAG, "qrcode is not enable");
                            Toast.makeText(this, "不支持选择的二维码,请选择支付宝/微信收款码", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        readTxt(currentPhotoString, type);

                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    /**
     * 压缩图片
     */
    private Bitmap resizePhono() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//仅仅加载图片
        BitmapFactory.decodeFile(currentPhotoString, options);
        if (options.outWidth < 1) {
            Log.w(LOG_TAG, "image not read" + currentPhotoString);
            return null;
        }
        double radio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
        options.inSampleSize = (int) Math.ceil(radio);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(currentPhotoString, options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                boolean writeExternalStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean readExternalStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (grantResults.length > 0 && writeExternalStorage && readExternalStorage) {
                    readImg();
                } else {
                    Toast.makeText(this, "请允许访问相册的权限", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private ImageClient imageClient;

    private void readTxt(final String file, final int type) {
        // Android 4.0 之后不能在主线程中请求HTTP请求
        new Thread(new Runnable() {
            @Override
            public void run() {

                String bucketName = "qrcode-1252008836";
                if (imageClient == null) {
                    String appId = "1252008836";
                    String secretId = "AKIDUJ5ZBdw7MivIfx7C82mLFe9aEiJksLxX";
                    String secretKey = "BB0ra9oOEwcOaeXdbTAg4LRMXouqgT6Y";
                    imageClient = new ImageClient(appId, secretId, secretKey);
                }
                File tagImage;
                try {
                    tagImage = new File(file);
                } catch (Exception ex) {
                    Log.w(LOG_TAG, ex);
                    return;
                }

                GeneralOcrRequest tagReq = new GeneralOcrRequest(bucketName, tagImage);

                String ret = imageClient.generalOcr(tagReq);


                QrCodeData qrData = new QrCodeData();
                try {
                    JSONObject jsonObject = new JSONObject(ret);
                    int code = jsonObject.getInt("code");
                    if (code != 0) {
                        Log.w(LOG_TAG, "image ORC code is " + code);
                        Log.w(LOG_TAG, ret);
                        return;
                    }
                    if (!jsonObject.has("data")) {
                        Log.w(LOG_TAG, "image ORC no data ");
                        return;
                    }
                    JSONArray array = jsonObject.getJSONObject("data").getJSONArray("items");
                    if (array == null) {
                        Log.w(LOG_TAG, "image ORC no items ");
                        return;
                    }
                    Pattern pMoney = Pattern.compile("￥([\\d\\.]+)");
                    if (type == QrCodeData.TYPE_WX) {
                        Pattern pWeixinNick = Pattern.compile("(\\S+)\\(\\*");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);
                            if (item.has("itemstring")) {
                                String value = item.getString("itemstring");
                                Matcher m = pMoney.matcher(value);
                                if (m.find()) {
                                    qrData.money = m.group(1);
                                    continue;
                                }
                                m = pWeixinNick.matcher(value);
                                if (m.find()) {
                                    qrData.userName = m.group(1);
                                    continue;
                                }
                            }
                        }
                    } else if (type == QrCodeData.TYPE_ALI) {
                        Pattern aliNick = Pattern.compile("支付宝|LIPAY");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);
                            if (item.has("itemstring")) {
                                String value = item.getString("itemstring");
                                Matcher m = pMoney.matcher(value);
                                if (m.find()) {
                                    qrData.money = m.group(1);
                                    continue;
                                }
                                m = aliNick.matcher(value);
                                if (m.find()) {
                                    continue;
                                } else {
                                    qrData.userName = value;
                                }
                            }
                        }
                    }
                    qrData.type = type;
                } catch (JSONException e) {
                    Log.w(LOG_TAG, e);
                }
                Log.i(LOG_TAG,qrData.money+qrData.userName);
            }
        }).start();
    }

}
