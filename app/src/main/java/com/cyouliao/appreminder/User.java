package com.cyouliao.appreminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class User {
    // 資料庫物件
    private SQLiteDatabase db;

    // 資料表名稱
    public static final String TABLE_NAME = "user";

    // 主鍵欄位
    public static final String KEY_ID = "_id";

    // 其他欄位
    // 後台資料庫的user.id
    public static final String U_ID_COLUMN = "u_id";
    // *unique* 參與者編號，心理系那邊設規則
    public static final String EXP_ID_COLUMN = "exp_id";
    // 1:成癮|0:非成癮
    public static final String ADDICTION_COLUMN = "addiction";
    // 實驗:training_strategy|對照:set_goal
    public static final String EXP_TYPE = "exp_type";
    // 進行任何操作皆要加到XHR，後台產生，不需讓參與者知道
    public static final String PASSWORD_COLUMN = "password";

    // 私有變數成員
    private long id;
    private int u_id = 0;
    private String exp_id;
    private int addiction;
    private String exp_type;
    private String password;

    // 使用以上宣告的變數建立資料表的SQL指令
    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    U_ID_COLUMN + " INTEGER NOT NULL, " +
                    EXP_ID_COLUMN + " TEXT NOT NULL, " +
                    ADDICTION_COLUMN + " INTEGER NOT NULL, " +
                    EXP_TYPE + " TEXT NOT NULL, " +
                    PASSWORD_COLUMN + " TEXT NOT NULL)";


    // 建構子 一般的應用不用修改
    public User(Context context) {
        db = DBHelper.getDatabase(context);
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE 1 LIMIT 1", null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                this.id =                   cursor.getLong(0);
                this.u_id =                 cursor.getInt(1);
                this.exp_id =               cursor.getString(2);
                this.addiction =            cursor.getInt(3);
                this.exp_type =             cursor.getString(4);
                this.password =             cursor.getString(5);
            } else {
                // 建立初始user資料
                ContentValues contentValues = new ContentValues();
                contentValues.put(U_ID_COLUMN, 0);
                contentValues.put(EXP_ID_COLUMN, "");
                contentValues.put(ADDICTION_COLUMN, 0);
                contentValues.put(EXP_TYPE, "");
                contentValues.put(PASSWORD_COLUMN, "");

                this.id = db.insert(TABLE_NAME, null, contentValues);
            }
        }
    }


    // 更新資料庫
    public void update() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(U_ID_COLUMN, u_id);
        contentValues.put(EXP_ID_COLUMN, exp_id);
        contentValues.put(ADDICTION_COLUMN, addiction);
        contentValues.put(EXP_TYPE, exp_type);
        contentValues.put(PASSWORD_COLUMN, password);

        String where = KEY_ID + "=" + this.id;
        db.update(TABLE_NAME, contentValues, where, null);
    }

    // 關閉資料庫
    public void close() { db.close(); }

    // 外部取得屬性方法
    public int getU_id() { return u_id; }

    public String getExp_id() {
        return exp_id;
    }

    public int getAddiction() {
        return addiction;
    }

    public String getExp_type() {
        return exp_type;
    }

    public String getPassword() {
        return password;
    }

    public void setU_id(int u_id) {
        this.u_id = u_id;
    }

    public void setExp_id(String exp_id) {
        this.exp_id = exp_id;
    }

    public void setAddiction(int addiction) {
        this.addiction = addiction;
    }

    public void setExp_type(String exp_type) {
        this.exp_type = exp_type;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // 檢查使用者登入狀態
    public boolean is_login() {
        if (u_id == 0) {
            return false;
        } else {
            return true;
        }
    }
}
