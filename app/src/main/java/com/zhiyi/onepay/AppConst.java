package com.zhiyi.onepay;

/**
 * Created by Administrator on 2018/8/6.
 */

public class AppConst {
    public static final String KeyAppId = "appid";
    public static final String KeyToken = "token";
    public static final String KeySecret = "secret";
    public static final String KeyBoolLog = "b_set_log";
//    public static final String KeyBoolWx = "b_set_wx";
//    public static final String KeyBoolZfb = "b_set_zfb";

    /**服务器地址*/
    public static final String HostUrl = "https://pxpay.ukafu.com/";

    /**传输数据为URL的KEY*/
    public static final String ACTION_URL = "a_url";

    public static int version;

    public static int Battery=0;

    public static boolean PlaySounds = true;

    public static int AppId=0;
    public static String Token="";
    public static String Secret="";

    public static final String TAG_LOG = "ZYKJ";

    public static final String IntentAction = "com.zhiyikeji.Notification";

    public static final int MT_Net_Response = 1;
    public static final int MT_Net_Toast = 1;

    public static final  String CHANNEL_ID          = "zhi_yi_px_pay";
    public static final  String CHANNEL_Front          = "zhi_yi_px_pay_front";
    public static final  String CHANNEL_Test         = "zhi_yi_px_pay_test";

    public static final String authUrl(String api){
        return HostUrl+api+"?appid="+AppId+"&token="+Token;
    }
}
