package com.zhiyi.onepay.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 

* @ClassName: DBHelper

* @Description: TODO(这里用一句话描述这个类的作用)

* @date 2018年6月23日 下午1:27:16

*
 */
public class DBHelper extends SQLiteOpenHelper{
	public DBHelper(Context context) {  
        super(context, "zykj.db", null, 1);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
//		db.execSQL("CREATE TABLE IF NOT EXISTS ukafu" +
//				"(_id INTEGER PRIMARY KEY AUTOINCREMENT, money varchar, mark varchar, type varchar, payurl varchar, dt varchar)");
		db.execSQL("CREATE TABLE IF NOT EXISTS ukafu (key_m varchar primary key,value_m varchar)");
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
