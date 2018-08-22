/**
 * 个人收款 https://gitee.com/DaLianZhiYiKeJi/xpay
 * 大连致一科技有限公司
 */

package com.zhiyi.onepay;

import android.app.Notification;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.zhiyi.onepay.util.AppUtil;
import com.zhiyi.onepay.util.RequestUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationMonitorService extends NotificationListenerService implements Handler.Callback {
    private static final String AliPay = "ALIPAY";
    private static final String WeixinPay = "WXPAY";
    //	private MyHandler handler;
    public long lastTimePosted = System.currentTimeMillis();
    private Pattern pAlipay;
    private Pattern pWeixin;
    private Handler callback;
    private MediaPlayer payComp;
    private MediaPlayer payRecv;


    public void onCreate() {
        super.onCreate();
        Log.i("ZYKJ", "Notification posted ");
        Toast.makeText(getApplicationContext(), "启动服务", Toast.LENGTH_LONG).show();
        //支付宝
        String pattern = "(\\S*)通过扫码向你付款([\\d\\.]+)元";
        pAlipay = Pattern.compile(pattern);
        pWeixin = Pattern.compile("微信支付收款([\\d\\.]+)元");
        callback = new Handler(this);
        payComp = MediaPlayer.create(this, R.raw.paycomp);
        payRecv = MediaPlayer.create(this, R.raw.payrecv);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, "default");
        nb.setContentTitle("PXPAY个人支付").setTicker("PXPAY个人支付").setSmallIcon(R.mipmap.ic_launcher);
        nb.setContentText("个人支付运行中.请保持此通知一直存在");
        //nb.setContent(new RemoteViews(getPackageName(),R.layout.layout));
        nb.setWhen(System.currentTimeMillis());
        Notification notification = nb.build();
        startForeground(1, notification);
    }

    public void onDestroy() {
        Intent localIntent = new Intent();
        localIntent.setClass(this, NotificationMonitorService.class);
        startService(localIntent);
    }

    public void onNotificationPosted(StatusBarNotification sbn) {
        Bundle bundle = sbn.getNotification().extras;
        String pkgName = sbn.getPackageName();
        if (getPackageName().equals(pkgName)) {
            //测试成功
            Log.i("ZYKJ", "测试成功");
            Intent intent = new Intent();
            intent.setAction(AppConst.IntentAction);
            Uri uri = new Uri.Builder().scheme("app").path("log").query("msg=测试成功").build();
            intent.setData(uri);
            sendBroadcast(intent);
            payRecv.start();
            return;
        }
        String title = bundle.getString("android.title");
        String text = ((Bundle) bundle).getString("android.text");
        Log.i("ZYKJ", "Notification posted [" + pkgName + "]:" + title + " & " + text);
        this.lastTimePosted = System.currentTimeMillis();
        //支付宝
        //com.eg.android.AlipayGphone]:支付宝通知 & 新哥通过扫码向你付款0.01元
        if (pkgName.equals("com.eg.android.AlipayGphone") && text != null) {
            // 现在创建 matcher 对象
            Matcher m = pAlipay.matcher(text);
            if (m.find()) {
                String uname = m.group(1);
                String money = m.group(2);
                postMethod(AliPay, money, uname);
            } else {
                //postError(AliPay, text);
            }
        }
        //微信
        //com.tencent.mm]:微信支付 & 微信支付收款0.01元
        else if (pkgName.equals("com.tencent.mm") && text != null) {
            // 现在创建 matcher 对象
            Matcher m = pWeixin.matcher(text);
            if (m.find()) {
                String uname = "微信用户";
                String money = m.group(1);
                postMethod(WeixinPay, money, uname);
            } else {
                //postError(WeixinPay, text);
            }
        }
    }


    public void onNotificationRemoved(StatusBarNotification paramStatusBarNotification) {
        Bundle localObject = paramStatusBarNotification.getNotification().extras;
        String pkgName = paramStatusBarNotification.getPackageName();
        String title = localObject.getString("android.title");
        String text = ((Bundle) localObject).getString("android.text");
        Log.i("ZYKJ", "Notification removed [" + pkgName + "]:" + title + " & " + text);
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        return START_STICKY;
    }

    /**
     * 获取道的支付通知发送到服务器
     *
     * @param payType
     * @param money
     * @param username
     */
    public void postMethod(String payType, String money, String username) {
//		Uri u = new Uri.Builder().path("pay").appendQueryParameter("type", payType)
//				.appendQueryParameter("money", money).appendQueryParameter("uname", username).build();
//		Intent intent = new Intent("notify",u);
//		sendBroadcast(intent);
//		String sec = "sec";

        payRecv.start();
        String app_id = "" + AppConst.AppId;
        String rndStr = AppUtil.randString(16);
        long time = System.currentTimeMillis() / 1000;
        int version = AppUtil.getVersionCode(this);
        String sign = AppUtil.toMD5(app_id + AppConst.Secret + time + version + rndStr + payType + money + username);
        RequestUtils.getRequest(AppConst.HostUrl + "person/notify/pay?type=" + payType
                        + "&money=" + money
                        + "&uname=" + username
                        + "&appid=" + app_id
                        + "&rndstr=" + rndStr
                        + "&sign=" + sign
                        + "&time=" + time
                        + "&version=" + version
                , callback);

    }

    /**
     * 发送错误信息到服务器
     *
     * @param payType
     * @param error
     */
    public void postError(String payType, String error) {
//		Uri u = new Uri.Builder().path("log")
//				.appendQueryParameter("msg", error).build();
//		Intent intent = new Intent("notify",u);
//		sendBroadcast(intent);
        RequestUtils.getRequest(AppConst.HostUrl + "person/notify/log?type=" + payType + "&error=" + error, callback);
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
            //发送通知的这个还有问题.接受不到,第一次写安卓,很多坑还不懂,求帮助
            Intent intent = new Intent();
            intent.setAction(AppConst.IntentAction);
            Uri uri = new Uri.Builder().scheme("app").path("pay").query("msg=支付完成&moeny=" + message.obj.toString()).build();
            intent.setData(uri);
            sendBroadcast(intent);
            JSONObject json;
            try {
                json = new JSONObject(msg);
                if (json.getInt("code") == 0) {
                    payComp.start();
                } else {
                    String emsg = json.getString("msg");
                    Log.w("ZYKJ", emsg);
                }

            } catch (JSONException e) {
                Log.w("ZYKJ", e);
            }

        } else if (what == AppConst.MT_Net_Toast) {
            Log.i("ZYKJ", message.obj.toString());
            //Toast.makeText(this, message.obj.toString(), Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}