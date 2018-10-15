/**
 *  个人收款 https://gitee.com/DaLianZhiYiKeJi/xpay
 *  大连致一科技有限公司
 * */

package com.zhiyi.onepay.util;


import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.zhiyi.onepay.AppConst;
import com.zhiyi.onepay.IHttpResponse;

//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;


public class RequestUtils {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient client;


    public static void getRequest(final String url,final IHttpResponse callback) {
        if(client == null){
            client = new OkHttpClient();
            client.setConnectTimeout(5, TimeUnit.SECONDS);
            client.setReadTimeout(5,TimeUnit.SECONDS);
            client.setWriteTimeout(5,TimeUnit.SECONDS);
        }
        Log.i("ZYKJ","request: "+url);
        Request request = new Request.Builder()
                .url(url).tag(url)
                .build();
            client.newCall(request).enqueue(new Callback(){

                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e("ZYKJ",e.getMessage()+":"+url);
                    callback.OnHttpDataError(e);
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    String rs = response.body().string();
                    Log.i("ZYKJ","response: "+rs);
                    callback.OnHttpData(rs);
                }
            });
//        Log.i("ZYKJ","request url:"+url);
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                //用HttpClient发送请求，分为五步
//                //第一步：创建HttpClient对象
//                HttpClient httpCient = new DefaultHttpClient();
//                //第二步：创建代表请求的对象,参数是访问的服务器地址
//                HttpGet httpGet = new HttpGet(url);
//                try {
//                    //第三步：执行请求，获取服务器发还的相应对象
//                    HttpResponse httpResponse = httpCient.execute(httpGet);
//                    //第四步：检查相应的状态是否正常：检查状态码的值是200表示正常
//                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
//                        //第五步：从相应对象当中取出数据，放到entity当中
//                        HttpEntity entity = httpResponse.getEntity();
//                        String response = EntityUtils.toString(entity,"utf-8");//将entity当中的数据转换为字符串
//
//                        //在子线程中将Message对象发出去
//                        if(handler!=null) {
//                            Message message = new Message();
//                            message.what = AppConst.MT_Net_Response;
//                            message.arg1 = arg;
//                            message.obj = response.toString();
//                            handler.sendMessage(message);
//                        }
//                    }else{
//                        Log.w("ZYKJ","http response error"+httpResponse.getStatusLine().getStatusCode());
//                    }
//                } catch (Exception e) {
//                    Log.w("ZYKJ",e);
//                }
//            }
//        }).start();//这个start()方法不要忘记了
    }

    public static String post(String url, String json){
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            Log.e("ZYKJ",e.getMessage());
        }
        return "";
    }
}