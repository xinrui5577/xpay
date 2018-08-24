package com.zhiyi.onepay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.decode.BitmapDecoder;
import com.qcloud.image.ImageClient;
import com.qcloud.image.request.GeneralOcrRequest;
import com.zhiyi.onepay.data.QrCodeData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class QrcodeActivity extends AppCompatActivity {

    private final int PICK_CODE = 2;

    private static String LOG_TAG = "ZYKJ";


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            readImg();
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_qrcode);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

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
                String currentPhotoString = cursor.getString(idx);
                cursor.close();
                Bitmap bitmap = resizePhono(currentPhotoString);
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
    private Bitmap resizePhono(String currentPhotoString ) {
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
