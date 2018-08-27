/**
 *  个人收款 https://gitee.com/DaLianZhiYiKeJi/xpay
 *  大连致一科技有限公司
 * */

package com.zhiyi.onepay.util;


import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.zhiyi.onepay.AppConst;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;




public class RequestUtils {



    public static void getRequest(final String url,final Handler handler) {
        getRequest(url,handler,0);
    }
    public static void getRequest(final String url,final Handler handler,final int arg) {
        Log.i("ZYKJ","request url:"+url);
        new Thread(new Runnable() {

            @Override
            public void run() {
                //用HttpClient发送请求，分为五步
                //第一步：创建HttpClient对象
                HttpClient httpCient = new DefaultHttpClient();
                //第二步：创建代表请求的对象,参数是访问的服务器地址
                HttpGet httpGet = new HttpGet(url);
                try {
                    //第三步：执行请求，获取服务器发还的相应对象
                    HttpResponse httpResponse = httpCient.execute(httpGet);
                    //第四步：检查相应的状态是否正常：检查状态码的值是200表示正常
                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        //第五步：从相应对象当中取出数据，放到entity当中
                        HttpEntity entity = httpResponse.getEntity();
                        String response = EntityUtils.toString(entity,"utf-8");//将entity当中的数据转换为字符串

                        //在子线程中将Message对象发出去
                        if(handler!=null) {
                            Message message = new Message();
                            message.what = AppConst.MT_Net_Response;
                            message.arg1 = arg;
                            message.obj = response.toString();
                            handler.sendMessage(message);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();//这个start()方法不要忘记了
    }
}