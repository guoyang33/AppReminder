package com.cyouliao.appreminder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "data.db";
    public static final int VERSION = 2;
    private static SQLiteDatabase database;

    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public static SQLiteDatabase getDatabase(Context context) {
        if (database==null || !database.isOpen()) {
            database = new DBHelper(context, DATABASE_NAME, null, VERSION).getWritableDatabase();
        }
        return database;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // 建立APP需要的資料表
        sqLiteDatabase.execSQL(User.CREATE_TABLE);
//        sqLiteDatabase.execSQL(AppCategory.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // 刪除原有的資料表
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + User.TABLE_NAME);
//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + AppCategory.TABLE_NAME);
        // 呼叫onCreate建立新版的資料表
        onCreate(sqLiteDatabase);
    }
}
