package com.zhiyi.onepay.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * @ClassName: DBManager
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @date 2018年6月23日 下午1:27:22
 */
public class DBManager {
    private SQLiteDatabase db;
    private DBHelper helper;
    private final String tableaName="ukafu";

    public DBManager(Context context) {
        helper = new DBHelper(context);
        db = helper.getWritableDatabase();
    }

    public String getConfig(String name) {
        Cursor c = db.query(tableaName,new String[]{"value_m"},"key_m=?",new String[]{name},null,null,null);
        String rs = "";
        while (c.moveToNext()) {
            rs = c.getString(c.getColumnIndex("value_m"));
            break;
        }
        c.close();
        return rs;
    }

    public boolean hasConfig(String name){
        Cursor c = db.query(tableaName,new String[]{"value_m"},"key_m=?",new String[]{name},null,null,null);
        return c.moveToNext();
    }

    public boolean setConfig(String name,String value){
        ContentValues values = new ContentValues();
        values.put("key_m",name);
        values.put("value_m",value);
        long n;
        if(hasConfig(name)){
            n = db.update(tableaName,values,"key_m=?",new String[]{name});
        }else{
            n = db.insert(tableaName,null,values);
        }
        return n>0;
    }

}
