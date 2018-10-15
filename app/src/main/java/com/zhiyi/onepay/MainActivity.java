/**
 *  个人收款 https://gitee.com/DaLianZhiYiKeJi/xpay
 *  大连致一科技有限公司
 * */

package com.zhiyi.onepay;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
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
    private Button btn_qrcode;
    private Button btn_merchant;
    private Button btn_log;
    private TextView textView;
    private DBManager dbm;

    private Handler handler;


//    private NotificationChannel mNotificationChannel;

    private MainService service;
    private IMessageHander msgHander = new IMessageHander() {
        @Override
        public void handMessage(Message msg) {
            Log.i(LogTag,msg.obj.toString());
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
    private BroadcastReceiver receiver = new BootRecevier() ;


    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swt_fuwu = (Switch)findViewById(R.id.p1);
        swt_service = (Switch)findViewById(R.id.service);

        btn_qrcode = (Button) findViewById(R.id.btn_qrcode);
        btn_merchant = (Button)findViewById(R.id.btn_merchant);
        btn_log = (Button)findViewById(R.id.btn_log);

        swt_service.setChecked(false);
        handler = new Handler();

        textView = (TextView)findViewById(R.id.textView_Help);
        dbm = new DBManager(this);

        try {
            AppConst.version = getPackageManager().getPackageInfo(getPackageName(),0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


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

        btn_qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openQrcode();
            }
        });
        btn_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LogActivity.class);
                startActivity(intent);
            }
        });

        textView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setData(Uri.parse(AppConst.HostUrl+"help.html"));//Url 就是你要打开的网址
                    intent.setAction(Intent.ACTION_VIEW);
                    startActivity(intent); //启动浏览器
            }
        });

        btn_merchant.setOnClickListener(
                new View.OnClickListener(){
                     @Override
                     public void onClick(View v) {
                         Intent intent = new Intent(MainActivity.this, MerchantActivity.class);
                         startActivity(intent);
                     }
                 }
        );

        Button btn_order = (Button)findViewById(R.id.btn_order);
        btn_order.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                intent.putExtra(AppConst.ACTION_URL,AppConst.HostUrl+"app/start/index.html#/token="+AppConst.Token+"/appid="+AppConst.AppId);
                startActivity(intent);
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
            return;
        }
        swt_service.setEnabled(true);
        //开启服务
        ComponentName name = startService(new Intent(this, NotificationMonitorService.class));
        if(name ==null) {
            swt_service.setChecked(false);
            Toast.makeText(getApplicationContext(), "服务开启失败", Toast.LENGTH_LONG).show();
            return;
        }

        toggleNotificationListenerService();
        swt_service.setChecked(true);

        //微信支付宝开启

    }


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


    private void openQrcode(){
        Intent intent = new Intent(MainActivity.this, QrcodeActivity.class);
        startActivity(intent);
    }

//    private void sendNotice(){
//        Log.i("ZYKJ","MainActivity Send Notice");
//        NotificationManager mNM =(NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            mNotificationChannel = new NotificationChannel(AppConst.CHANNEL_Test, "pxapy", NotificationManager.IMPORTANCE_DEFAULT);
//            mNotificationChannel.setDescription("个人支付的监控");
//            mNM.createNotificationChannel(mNotificationChannel);
//            Log.i("ZYKJ","MainActivity create mNotificationChannel");
//        }
//        Notification notification = new NotificationCompat.Builder(this,AppConst.CHANNEL_Test)
//                .setContentTitle("通知监控").setContentText("测试号通过扫码向你付款0.01元").setSmallIcon(R.mipmap.ic_launcher)
//                .setWhen(System.currentTimeMillis()).setChannelId(AppConst.CHANNEL_Test).build();
//
//        /**
//         * 注意,我们使用出来。incoming_message ID 通知。它可以是任何整数,但我们使用 资源id字符串相关
//         * 通知。它将永远是一个独特的号码在你的 应用程序。
//         */
//        mNM.notify(2,notification);
//        Log.i("ZYKJ","MainActivity Send Notice Comp");
//    }

    private void toggleNotificationListenerService()
    {
        PackageManager localPackageManager = getPackageManager();
        localPackageManager.setComponentEnabledSetting(new ComponentName(this, NotificationMonitorService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        localPackageManager.setComponentEnabledSetting(new ComponentName(this, NotificationMonitorService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }
//
//    private void openPowerSetting(){
//        Intent powerUsageIntent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
//        ResolveInfo resolveInfo = getPackageManager().resolveActivity(powerUsageIntent, 0);
//        if(resolveInfo != null){
//            startActivity(powerUsageIntent);
//        }
//    }

    /**
     * 打开通知权限设置.一般手机根本找不到哪里设置
     */
    private void openNotificationListenSettings()
    {

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
