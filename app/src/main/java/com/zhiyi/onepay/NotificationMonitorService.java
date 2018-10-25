/**
 * 个人收款 https://gitee.com/DaLianZhiYiKeJi/xpay
 * 大连致一科技有限公司
 */

package com.zhiyi.onepay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.zhiyi.onepay.components.BatteryReceiver;
import com.zhiyi.onepay.util.AppUtil;
import com.zhiyi.onepay.util.DBManager;
import com.zhiyi.onepay.util.RequestUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationMonitorService extends NotificationListenerService implements Runnable {
    private static final String AliPay = "ALIPAY";
    private static final String WeixinPay = "WXPAY";
    //	private MyHandler handler;
    public long lastTimePosted = System.currentTimeMillis();
    private Pattern pAlipay;
    private Pattern pAlipay2;
    private Pattern pWeixin;
    private MediaPlayer payComp;
    private MediaPlayer payRecv;
    private MediaPlayer payNetWorkError;
    private PowerManager.WakeLock wakeLock;
    private DBManager dbManager;


    public void onCreate() {
        super.onCreate();
        Log.i("ZYKJ", "Notification posted ");
        Toast.makeText(getApplicationContext(), "启动服务", Toast.LENGTH_LONG).show();
        //支付宝
        String pattern = "(\\S*)通过扫码向你付款([\\d\\.]+)元";
        pAlipay = Pattern.compile(pattern);
        pattern = "成功收款([\\d\\.]+)元。享免费提现等更多专属服务，点击查看";
        pAlipay2 = Pattern.compile(pattern);
        pWeixin = Pattern.compile("微信支付收款([\\d\\.]+)元");
        payComp = MediaPlayer.create(this, R.raw.paycomp);
        payRecv = MediaPlayer.create(this, R.raw.payrecv);
        payNetWorkError = MediaPlayer.create(this, R.raw.networkerror);
        dbManager = new DBManager(this);
        if (AppConst.AppId < 1) {
            String appid = dbManager.getConfig(AppConst.KeyAppId);
            if (!TextUtils.isEmpty(appid)) {
                AppConst.AppId = Integer.parseInt(appid);
                String token = dbManager.getConfig(AppConst.KeyToken);
                if (!TextUtils.isEmpty(token)) {
                    AppConst.Token = token;
                }
                String secret = dbManager.getConfig(AppConst.KeySecret);
                if (!TextUtils.isEmpty(secret)) {
                    AppConst.Secret = secret;
                }
            }
        }
        new Thread(this).start();
        Log.i("ZYKJ", "Notification Monitor Service start");
        NotificationManager mNM = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mNM != null) {
            NotificationChannel mNotificationChannel = mNM.getNotificationChannel(AppConst.CHANNEL_ID);
            if (mNotificationChannel == null) {
                mNotificationChannel = new NotificationChannel(AppConst.CHANNEL_ID, "pxapy", NotificationManager.IMPORTANCE_DEFAULT);
                mNotificationChannel.setDescription("个人支付的监控");
                mNM.createNotificationChannel(mNotificationChannel);
            }
        }
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, AppConst.CHANNEL_ID);//

        nb.setContentTitle("PXPAY个人支付").setTicker("PXPAY个人支付").setSmallIcon(R.drawable.icon);
        nb.setContentText("个人支付运行中.请保持此通知一直存在");
        //nb.setContent(new RemoteViews(getPackageName(),R.layout.layout));
        nb.setWhen(System.currentTimeMillis());
        Notification notification = nb.build();
        startForeground(1, notification);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //保持cpu一直运行，不管屏幕是否黑屏
        if (pm != null && wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getCanonicalName());
            wakeLock.acquire();
        }

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        BatteryReceiver batteryReceiver = new BatteryReceiver();
        registerReceiver(batteryReceiver, intentFilter);

        Log.i("ZYKJ", "Notification Monitor Service started");
    }


    public void onDestroy() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
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
        String text = bundle.getString("android.text");
        Log.i("ZYKJ", "Notification posted [" + pkgName + "]:" + title + " & " + text);
        this.lastTimePosted = System.currentTimeMillis();
        //支付宝com.eg.android.AlipayGphone
        //com.eg.android.AlipayGphone]:支付宝通知 & 新哥通过扫码向你付款0.01元
        if (pkgName.equals("com.eg.android.AlipayGphone") && text != null) {
            // 现在创建 matcher 对象
            Matcher m = pAlipay.matcher(text);
            if (m.find()) {
                String uname = m.group(1);
                String money = m.group(2);
                postMethod(AliPay, money, uname);
            } else {
                m = pAlipay2.matcher(text);
                if (m.find()) {
                    String money = m.group(1);
                    postMethod(AliPay, money, "支付宝用户");
                } else {
                    Log.w("ZYKJ", "匹配失败" + text);
                }
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
            }
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e("ZYKJ", "service thread", e);
            }
            long now = System.currentTimeMillis();
            //10秒内有交互,取消
            if(now - lastNetTime<10){
                return;
            }
            //发送在线通知,保持让系统时时刻刻直到app在线,5秒发送一次
            if(now-lastSendTime<5000){
                return;
            }
            postState();
            //20秒,没消息了.提示网络异常
            if (now - lastNetTime > 20000) {
                playMedia(payNetWorkError);
            }
        }
    }


    public void onNotificationRemoved(StatusBarNotification paramStatusBarNotification) {
        if (Build.VERSION.SDK_INT >= 19) {
            Bundle localObject = paramStatusBarNotification.getNotification().extras;
            String pkgName = paramStatusBarNotification.getPackageName();
            String title = localObject.getString("android.title");
            String text = (localObject).getString("android.text");
            Log.i("ZYKJ", "Notification removed [" + pkgName + "]:" + title + " & " + text);
        }
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        return START_STICKY;
    }

    /**
     * 获取道的支付通知发送到服务器
     *
     * @param payType  支付方式
     * @param money    支付金额
     * @param username 支付者名字
     */
    public void postMethod(final String payType, final String money, final String username) {
//		Uri u = new Uri.Builder().path("pay").appendQueryParameter("type", payType)
//				.appendQueryParameter("money", money).appendQueryParameter("uname", username).build();
//		Intent intent = new Intent("notify",u);
//		sendBroadcast(intent);
//		String sec = "sec";
        dbManager.addLog("new order:" + payType + "," + money + "," + username, 101);
        playMedia(payRecv);
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
                , new IHttpResponse() {
                    @Override
                    public void OnHttpData(String data) {
                        dbManager.addLog(data, 200);
                        handleMessage(data, 1);
                    }

                    @Override
                    public void OnHttpDataError(IOException e) {
                        dbManager.addLog("http error," + payType + "," + money + "," + username + ":" + e.getMessage(), 500);
                    }
                });

    }

    private long lastSendTime;

    /**
     * 发送错误信息到服务器
     */
    public void postState() {
        lastSendTime = System.currentTimeMillis();
        RequestUtils.getRequest(AppConst.authUrl("person/state/online") + "&v=" + AppConst.version + "&b=" + AppConst.Battery, new IHttpResponse() {
            @Override
            public void OnHttpData(String data) {
                handleMessage(data, 3);
            }

            @Override
            public void OnHttpDataError(IOException e) {

            }
        });
    }

    private long lastNetTime;

    public boolean handleMessage(String message, int arg1) {
        lastNetTime = System.currentTimeMillis();
        if (message == null || message.isEmpty()) {
            return true;
        }
        if (arg1 == 3) {

            return true;
        }
        String msg = message;
        Log.i("ZYKJ", msg);
        //发送通知的这个还有问题.接受不到,第一次写安卓,很多坑还不懂,求帮助
//            Intent intent = new Intent(NotificationMonitorService.this,MainActivity.class);
//            intent.setAction(AppConst.IntentAction);
//            Uri uri = new Uri.Builder().scheme("app").path("pay").query("msg=支付完成&moeny=" + message.obj.toString()).build();
//            intent.setData(uri);
//            sendBroadcast(intent);
        JSONObject json;
        try {
            json = new JSONObject(msg);
            if (json.getInt("code") == 0) {
                playMedia(payComp);
            } else {
                String emsg = json.getString("msg");
                Log.w("ZYKJ", emsg);
            }

        } catch (JSONException e) {
            Log.w("ZYKJ", e);
        }

        return true;
    }

    private void playMedia(MediaPlayer media) {
        if (AppConst.PlaySounds) {
            media.start();
        }
    }
}