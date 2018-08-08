package com.zhiyi.onepay;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class MainService extends Service implements Runnable{
    private Handler handler;
    private IMessageHander msgHander;
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
            handler.handleMessage(msg);
//            payCompSounds();
        }
    }

//    private MediaPlayer payComp;
//    public void payCompSounds(){
//        if(payComp == null) {
//            payComp = MediaPlayer.create(this, R.raw.paycomp);
//        }
//        payComp.start();
//    }
}

