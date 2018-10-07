package com.zhiyi.onepay;

import android.app.AlertDialog;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zhiyi.onepay.util.RequestUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class MerchantActivity extends AppCompatActivity implements Handler.Callback{
    private Button btn_copy;
    private Button btn_reset;
    private TextView txt_Merchant;
    private TextView txt_Secret;
    private String merchantId;
    private String Secret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant);
        btn_copy = findViewById(R.id.button_copy);
        btn_reset = findViewById(R.id.button_reset);
        txt_Merchant = findViewById(R.id.merchant_id);
        txt_Secret = findViewById(R.id.merchant_secret);

        btn_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copySecret();
            }
        });

        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog  dialog = new AlertDialog.Builder(MerchantActivity.this).setTitle("确定要重置?").setIcon(R.drawable.icon).
                        setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RequestUtils.getRequest(AppConst.authUrl("person/merchant/resetSecret"),new Handler(MerchantActivity.this),2);
                    }
                }).show();
                dialog.setCancelable(true);
            }
        });
        RequestUtils.getRequest(AppConst.authUrl("person/merchant/getMerchant"),new Handler(this),1);
    }

    private void copySecret(){
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData mClipData = ClipData.newPlainText("Label", "商户ID: "+merchantId+" ,密钥: "+Secret);
        cm.setPrimaryClip(mClipData);
        Toast.makeText(this, "复制成功", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean handleMessage(Message message) {
        int what = message.what;
        if (what == AppConst.MT_Net_Response) {
            if (message.obj == null) {
                return true;
            }
            String msg = message.obj.toString();
            Log.i("ZYKJ", msg);

            JSONObject json;
            try {
                json = new JSONObject(msg);
                if (json.getInt("code") == 0) {
                    json = json.getJSONObject("data");
                    merchantId = json.getString("merchantid");
                    Secret = json.getString("secret");
                    txt_Merchant.setText(merchantId);
                    txt_Secret.setText(Secret);
                } else {
                    String emsg = json.getString("msg");
                    Log.w("ZYKJ", emsg);
                    Toast.makeText(this, emsg, Toast.LENGTH_LONG).show();
                }

            } catch (JSONException e) {
                Log.w("ZYKJ", e);
            }
        }
        return true;
    }
}
