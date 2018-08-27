package com.zhiyi.onepay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.decode.BitmapDecoder;
import com.qcloud.image.ImageClient;
import com.qcloud.image.request.GeneralOcrRequest;
import com.zhiyi.onepay.data.QrCodeData;
import com.zhiyi.onepay.util.RequestUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class QrcodeActivity extends AppCompatActivity implements Handler.Callback {

    public final int ARG_TYPE_QRCODELIST = 1;
    public final int ARG_TYPE_ADD = 2;
    public final int ARG_TYPE_DEL = 3;


    private final int PICK_CODE = 2;

    private static String LOG_TAG = "ZYKJ";

    private View mContentView;
    private LinearLayout container;

    private Handler handler;

    private View.OnClickListener readClick = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            readImg();
        }
    };
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_qrcode);

        container = findViewById(R.id.container);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(readClick);

        handler = new Handler(getMainLooper(), this);

        findViewById(R.id.dummy_button).setOnClickListener(readClick);

        RequestUtils.getRequest(AppConst.authUrl("person/qrcode/index"), handler, ARG_TYPE_QRCODELIST);
    }


    public void readImg() {
//        Intent pic = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(pic, PICK_CODE);

        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.putExtra("scale", true);//设置可以缩放
        intent.putExtra("crop", true);//设置可以裁剪
        intent.setType("image/*");//设置需要从系统选择的内容：图片
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, this.imageUri);//设置输出位置
        startActivityForResult(intent, PICK_CODE);
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
                        readTxt(currentPhotoString, txt);

                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    /**
     * 压缩图片
     */
    private Bitmap resizePhono(String currentPhotoString) {
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

    private void readTxt(final String file,final String url) {
        final int type;
        if (url.toUpperCase().startsWith("WXP://")) {
            type = QrCodeData.TYPE_WX;
        } else if (url.toUpperCase().startsWith("HTTPS://QR.ALIPAY.COM")) {
            type = QrCodeData.TYPE_ALI;
        } else {
            Log.w(LOG_TAG, "qrcode is not enable");
            Toast.makeText(this, "不支持选择的二维码,请选择支付宝/微信收款码", Toast.LENGTH_SHORT).show();
            return;
        }
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
                qrData.money = "0";
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
                                    qrData.name = m.group(1);
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
                                    qrData.name = value;
                                }
                            }
                        }
                    }
                    if(!qrData.name.matches("^\\S+$")){
                        qrData.name = "";
                    }
                    qrData.type = type;
                } catch (JSONException e) {
                    Log.w(LOG_TAG, e);
                }

                Log.i(LOG_TAG, qrData.money + qrData.name);
                RequestUtils.getRequest(AppConst.authUrl("person/qrcode/upload")+"&money="+qrData.money+"&name="+qrData.name+"&code="+url,handler,ARG_TYPE_ADD);
            }
        }).start();
    }

    private HashMap<String,View> codeMap = new HashMap<>();
    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == AppConst.MT_Net_Response) {
            if (msg.arg1 == ARG_TYPE_QRCODELIST) {
                try {
                    JSONObject obj = new JSONObject(msg.obj.toString());
                    if (obj.getInt("code") == 0) {
                        JSONArray list = obj.getJSONArray("data");
                        int[] colors = new int[]{R.color.colorPrimary,R.color.colorPrimaryDark};
                        if (list != null && list.length() > 0) {
                            int flag = 0;
                            for (int i = 0; i < list.length(); i++) {
                                View view = addViewItem(list.getJSONObject(i));
                                if(view!=null){
                                    view.setBackgroundColor(getResources().getColor(colors[++flag%2],getTheme()));
                                }
                            }
                        }

                    } else {
                        Toast.makeText(this, obj.getString("msg"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else if(msg.arg1 == ARG_TYPE_ADD){
                try {
                    JSONObject obj = new JSONObject(msg.obj.toString());
                    if (obj.getInt("code") == 0) {
                        JSONObject data = obj.getJSONObject("data");
                        updateView(data);
                    } else {
                        Toast.makeText(this, obj.getString("msg"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if(codeMap.size()>0){
                mContentView.setVisibility(View.VISIBLE);
            }
        }
        return false;
    }

    private View addViewItem(JSONObject data) {

        try {
            if(!data.has("money_round")){
                return null;
            }
            Object o = data.get("money_round");
            String money = ""+o;
            if(codeMap.containsKey(money)){
                updateView(data);
                return null;
            }
            View view = View.inflate(this, R.layout.qrcode, null);

            TextView btn_add = view.findViewById(R.id.qr_money);
            btn_add.setText(money);
            updateText(data,view);
            codeMap.put(money,view);
            container.addView(view);
            return view;
        } catch (JSONException e) {
            Log.e(LOG_TAG,"获取money_round失败"+data.toString());
            Log.w(LOG_TAG,e);
            return null;
        }

    }

    public void updateView(JSONObject data){
        try {
            if(!data.has("money_round")){
                return;
            }
            Object o = data.get("money_round");
            String key = ""+o;
            if(data.has("add")) {
                int add = data.getInt("add");
                if (add == 0) {
                    return;
                }
            }
            if(codeMap.containsKey(key)){
                View view = codeMap.get(key);
                updateText(data,view);
            }else{
                addViewItem(data);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG,"获取money_round失败"+data.toString());
            Log.w(LOG_TAG,e);
        }
    }

    private void updateText(JSONObject data,View view) throws JSONException {
        String type = data.getString("pay_type");
        TextView textView = null;
        if(QrCodeData.NAME_ALI.equals(type)){
             textView = view.findViewById(R.id.qr_zfb_num);

        }else if(QrCodeData.NAME_WX.equals(type)) {
            textView = view.findViewById(R.id.qr_wx_num);
        }
        int num=0;
        if(data.has("count")){
            num = data.getInt("count");
        }else{
            String numStr = textView.getText().toString();
            num = Integer.parseInt(numStr)+1;
        }
        textView.setText(num+"");
    }
}
