/**
 *  个人收款 https://gitee.com/DaLianZhiYiKeJi/xpay
 *  大连致一科技有限公司
 * */


package com.zhiyi.onepay;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

/**
 * 后台进程.确保进入后台也在运行
 */
public class MainService extends Service implements Runnable, MediaPlayer.OnCompletionListener {
    private Handler handler;
    private IMessageHander msgHander;
    private MediaPlayer payComp;
    private PowerManager.WakeLock wakeLock;
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if(msgHander!=null){
                    msgHander.handMessage(msg);
                }
            }
        };
        new Thread(this,"MainService").start();
        //保持黑屏状态执行
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MainService.class.getName());
        if(wakeLock!=null){
            wakeLock.acquire();
        }else{
            Log.w("ZYKJ","wakeLock is null");
        }
        //声音播放也不成功
        payComp = MediaPlayer.create(this, R.raw.paycomp);
        payComp.setOnCompletionListener(this);
//        wakeLock.acquire();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void setMessageHander(IMessageHander hander){
        msgHander = hander;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    class MyBinder extends Binder{
        public MainService getService() {
            return MainService.this;
        }
    }

    @Override
    public void run() {
        while(true){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log.e("ZYKJ","service thread",e);
            }
            Message msg = new Message();
            msg.what = 1;
            msg.obj = "time";
            handler.sendMessage(msg);
        }
    }


    /**
     * 支付通知发送成功的时候.播报声音.
     * 在听到 微信到账1元之后.听到支付已完成,就知道系统没毛病.如果不能同时听到2声音.表示又问题了..
     * 但是这个播放声音我的手机老是出毛病.重启后就好了..
     */
    public void payCompSounds(){
        payComp.start();
    }


    /**
     * 据说这样能提高存活率,貌似也不太稳定
     */
    @Override
    public void onDestroy() {
        if(payComp!=null){
            payComp.release();
            payComp= null;
        }
        if(wakeLock!=null){
            wakeLock.release();
            wakeLock = null;
        }
        Intent localIntent = new Intent();
        localIntent.setClass(this, MainService.class);
        startService(localIntent);
    }
}

