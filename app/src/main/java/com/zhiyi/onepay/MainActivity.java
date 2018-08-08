/**
 *  个人收款 https://gitee.com/DaLianZhiYiKeJi/xpay
 *  大连致一科技有限公司
 * */

package com.zhiyi.onepay;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.zhiyi.onepay.util.DBManager;

public class MainActivity extends AppCompatActivity {
    private final static String TRUE = "true";
    private final static String FALSE = "false";
    private final static String LogTag = "ZYKJ";

    private Switch swt_fuwu;
    private Switch swt_service;
    private Switch swt_log;
    private Switch swt_wx;
    private Switch swt_zfb;
    private Button btn_test;
    private DBManager dbm;
    private TextView logView;

    private boolean enableWx;
    private boolean enableZfb;
    private boolean enableLog;
    private Handler handler;

    private MainService service;
    private IMessageHander msgHander = new IMessageHander() {
        @Override
        public void handMessage(Message msg) {
            Log.i(LogTag,msg.obj.toString());
            if(enableLog){
                appendLog(msg.obj.toString());
            }
        }
    };
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MainService.MyBinder myBinder = (MainService.MyBinder) binder;
            service = myBinder.getService();
            service.setMessageHander(msgHander);
            Log.i(LogTag, "MainActive - onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(LogTag, "MainActive - onServiceDisconnected");
        }
    };

    //不知到什么原因.广播收不到
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri uri = intent.getData();
            Log.i(LogTag,"broadcast"+uri.toString());
            String path = uri.getPath();
            if("log".equals(path)){
                appendLog(uri.toString());
            }
        }
    };


    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swt_fuwu = findViewById(R.id.p1);
        swt_service = findViewById(R.id.service);
        swt_log = findViewById(R.id.log);
        swt_wx = findViewById(R.id.wx);
        swt_zfb = findViewById(R.id.zfb);
        btn_test = findViewById(R.id.btn_test);
        logView = findViewById(R.id.text_log);
        swt_service.setChecked(false);
        handler = new Handler();
        sb = new StringBuilder(1200);

        dbm = new DBManager(this);
        String log = dbm.getConfig(AppConst.KeyBoolLog);
        enableLog = TRUE.equals(log);
        swt_log.setChecked(enableLog);

        String wx = dbm.getConfig(AppConst.KeyBoolWx);
        enableWx = TRUE.equals(wx);
        swt_wx.setChecked(enableWx);

        String zfb = dbm.getConfig(AppConst.KeyBoolZfb);
        enableZfb = TRUE.equals(zfb);
        swt_zfb.setChecked(enableZfb);
        IntentFilter filter = new IntentFilter(AppConst.IntentAction);
        registerReceiver(receiver,filter);

        swt_fuwu.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked != enabedPrivileges){
                    openNotificationListenSettings();
                }
            }
        });

        swt_service.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    checkStatus();
                }
            }
        });
        swt_log.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(enableLog!=isChecked) {
                    enableLog = isChecked;
                    dbm.setConfig(AppConst.KeyBoolLog, enableLog ? TRUE : FALSE);
                    sb.delete(0,sb.length());
                    logView.setText("");
                }
            }
        });
        swt_wx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(enableWx!=isChecked) {
                    enableWx = isChecked;
                    dbm.setConfig(AppConst.KeyBoolWx, enableWx ? TRUE : FALSE);
                }
            }
        });
        swt_zfb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(enableZfb!=isChecked) {
                    enableZfb = isChecked;
                    dbm.setConfig(AppConst.KeyBoolZfb, enableZfb ? TRUE : FALSE);
                }
            }
        });
        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendNotice();
            }
        });
        Intent intent = new Intent(this, MainService.class);
        intent.putExtra("from", "MainActive");
        bindService(intent, conn, BIND_AUTO_CREATE);
        checkStatus();
    }



    @Override
    protected void onResume() {
        super.onResume();
        checkStatus();
    }
    private boolean enabedPrivileges;
    private void checkStatus(){
        //权限开启.才能启动服务
        boolean enabled = isEnabled();
        enabedPrivileges = enabled;
        swt_fuwu.setChecked(enabled);
        if(!enabled){
            swt_service.setEnabled(false);
            swt_log.setEnabled(false);
            swt_wx.setEnabled(false);
            swt_zfb.setEnabled(false);
            appendLog("权限未开启");
            return;
        }
        swt_service.setEnabled(true);
        swt_log.setEnabled(true);
        //开启服务
        ComponentName name = startService(new Intent(this, NotificationMonitorService.class));
        if(name ==null) {
            swt_service.setChecked(false);
            Toast.makeText(getApplicationContext(), "服务开启失败", Toast.LENGTH_LONG).show();
            appendLog("服务开启失败");
            return;
        }
        appendLog("服务开启成功");

        toggleNotificationListenerService();
        swt_service.setChecked(true);
        //微信支付宝开启
        swt_wx.setEnabled(true);
        swt_zfb.setEnabled(true);
    }
    private StringBuilder sb;
    private void appendLog(String log){
        if(enableLog){
            if(sb.length()>1000){
                sb = new StringBuilder(1200);
                sb.append("清理日志\n");
            }
            sb.append(log+"\n");
            handler.post(runnableUi);
        }
    }

    Runnable   runnableUi=new  Runnable(){
        @Override
        public void run() {
            //更新界面
            logView.setText(sb.toString());
        }

    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            keyCode = KeyEvent.KEYCODE_HOME;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isEnabled()
    {
        String str = getPackageName();
        String localObject = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(localObject))
        {
            String[] strArr = (localObject).split(":");
            int i = 0;
            while (i < strArr.length)
            {
                ComponentName localComponentName = ComponentName.unflattenFromString(strArr[i]);
                if ((localComponentName != null) && (TextUtils.equals(str, localComponentName.getPackageName())))
                    return true;
                i += 1;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void sendNotice(){
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this,"default");
        nb.setContentTitle("通知监控").setContentText("测试号通过扫码向你付款0.01元").setSmallIcon(R.mipmap.ic_launcher);
        nb.setWhen(System.currentTimeMillis());
        Notification notification = nb.build();
        NotificationManager mNM =(NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
        /**
         * 注意,我们使用出来。incoming_message ID 通知。它可以是任何整数,但我们使用 资源id字符串相关
         * 通知。它将永远是一个独特的号码在你的 应用程序。
         */
        mNM.notify(1,notification);
    }

    private void toggleNotificationListenerService()
    {
        PackageManager localPackageManager = getPackageManager();
        localPackageManager.setComponentEnabledSetting(new ComponentName(this, NotificationMonitorService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        localPackageManager.setComponentEnabledSetting(new ComponentName(this, NotificationMonitorService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * 打开通知权限设置.一般手机根本找不到哪里设置
     */
    private void openNotificationListenSettings()
    {
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent localIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(localIntent);
            }
        }
        catch (Exception localException)
        {
            localException.printStackTrace();
        }

        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            } else {
                intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            }
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
