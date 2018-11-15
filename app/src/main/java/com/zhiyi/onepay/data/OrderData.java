package com.zhiyi.onepay.data;

import com.zhiyi.onepay.AppConst;

/**
 * Created by Administrator on 2018/11/4.
 */

public class OrderData {
    public String payType;
    public String username;
    public String money;
    public String sign;
    public String ranStr;
    public int dianYuan;
    public long time;
    public OrderData(final String payType, final String money, final String username,boolean dianYuan){
        this.payType = payType;
        this.username = username;
        this.money = money;
        this.dianYuan = dianYuan?1:0;
        time = System.currentTimeMillis()/1000 - AppConst.DetaTime;
    }
}
