package com.cyouliao.appreminder;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;


public class MainActivity extends AppCompatActivity {

    private User user;
    private AppUsage appUsage;
    private Notify notify;

    Button buttonEnter;
    Button buttonDataUpload;

    private String DATA_MAKEUP_CHANNEL_ID = "DataMakeup";
    private String TODO_CHANNEL_ID = "Todo";
    private List<String> dataMakeupDateList;
    private List<String> todoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonEnter = (Button) findViewById(R.id.enter_button);
        buttonDataUpload = (Button) findViewById(R.id.data_upload_button);

        dataMakeupDateList = new ArrayList<>();
        todoList = new ArrayList<>();

        // 加入channel
        notify = new Notify();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notify.getChannelDataMakeup());
            notificationManager.createNotificationChannel(notify.getChannelTodo());
        }

        // 檢查 Usage access 權限
        AppOpsManager appOps = (AppOpsManager) this.getSystemService(this.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), this.getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        if (!granted) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                try {
                    // 開啟 Usage access 權限設定畫面
                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle("使用記錄權限");
                    alertDialog.setMessage("請將健康上網APP設為允許");
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "設定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            // 強制重開
                            finish();
                        }
                    });
                    alertDialog.show();
                } catch (Exception e) {
                    Toast.makeText(this, "無法開啟允許查看使用情況應用界面", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        } else {        // 已獲得usage access權限
            // 類別實例化
            user = new User(this);

            // 檢查登入狀態
            if (!user.is_login()) {     // 未登入
                // 顯示登入界面
                Intent intent = new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                // 取得user_status
                Toast.makeText(this, "正在更新資料", Toast.LENGTH_LONG).show();
                fetchUserStatus();

                // 設定定時器
                // 取得狀態資訊
                setTimer();

                buttonEnter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, WebActivity.class);
                        startActivity(intent);
                    }
                });

                // 手動上傳資料按鈕
                buttonDataUpload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, DataUploadActivity.class);
                        startActivity(intent);
//                        setButtonDataUpdateEnabled(false);
                    }
                });

                // 跳轉至WebView
                Intent intent = new Intent(MainActivity.this, WebActivity.class);
                startActivity(intent);
            }
        }
    }

    // 計時器
    public void setTimer() {
        Intent intent = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 10 * 60 * 1000, pendingIntent);        // 10分鐘
    }

    // 取得user status
    public void fetchUserStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CloseableHttpClient client = HttpClients.createDefault();
                    HttpPost httpPost = new HttpPost("http://120.108.111.131/App_3rd/user_status.php");
                    List<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("u_id", "" + user.getU_id()));
                    params.add(new BasicNameValuePair("password", user.getPassword()));
                    httpPost.setEntity(new UrlEncodedFormEntity(params));
                    CloseableHttpResponse response = client.execute(httpPost);
                    assert response.getStatusLine().getStatusCode() == 200;
                    // 轉成JSON Object
                    JSONObject jsonResponse = new JSONObject(EntityUtils.toString(response.getEntity()));
                    JSONObject jsonHeaders = jsonResponse.getJSONObject("headers");
                    String status = jsonHeaders.getString("status");
                    if (!status.equals("OK")) {     // NOT OK
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle(status);
                        alertDialog.setMessage(jsonHeaders.getString("error_msg") + "\n請聯絡研究人員");
                        alertDialog.setCanceledOnTouchOutside(false);
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "關閉", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        alertDialog.show();
                    } else {        // OK
                        JSONObject contents = jsonResponse.getJSONObject("contents");
                        // 檢查缺少日期
                        JSONArray dataMakeupDateList = contents.getJSONArray("makeup_date");
                        if (dataMakeupDateList.length() > 0) {      // 尚有資料未上傳
                            List<String> dateList = new ArrayList<>();
                            for (int i = 0; i < dataMakeupDateList.length(); i++) {
                                dateList.add(dataMakeupDateList.getString(i));
                            }
                            // 上傳資料
                            dataUpload(dateList);
                        }
                    }
                    client.close();
                } catch (AssertionError e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "連線錯誤，請確認網路是否正常連線", Toast.LENGTH_LONG).show();
                        }
                    });
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void dataUpload(List<String> dataMakeupDateList) {
        // 類別實例化
        appUsage = new AppUsage(this, (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "正在上傳資料", Toast.LENGTH_LONG).show();
            }
        });

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("u_id", "" + user.getU_id()));
        params.add(new BasicNameValuePair("password", user.getPassword()));
        int usageCount = 0;
        for (String date :dataMakeupDateList) {
            List<UsageStats> usageStatsList = appUsage.queryUsageStatsByDate(date);
            for (UsageStats usageStats :usageStatsList) {
                Long usageTime = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    usageTime = (usageStats.getTotalTimeVisible()-usageStats.getTotalTimeVisible()%1000)/1000;
                } else {
                    usageTime = (usageStats.getTotalTimeInForeground()-usageStats.getTotalTimeInForeground()%1000)/1000;
                }
                if (usageTime > 0) {
                    String paramKey = "usage_data[" + date + "][" + usageStats.getPackageName() + "]";
                    params.add(new BasicNameValuePair(paramKey, usageTime.toString()));
                    usageCount++;
                }
            }
        }
        if (usageCount > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CloseableHttpClient client = HttpClients.createDefault();
                        HttpPost httpPost = new HttpPost("http://120.108.111.131/App_3rd/data_upload.php");
                        httpPost.setEntity(new UrlEncodedFormEntity(params));
                        CloseableHttpResponse response = client.execute(httpPost);
                        assert response.getStatusLine().getStatusCode() == 200;
                        JSONObject jsonResponse = new JSONObject(EntityUtils.toString(response.getEntity()));
                        JSONObject jsonHeaders = jsonResponse.getJSONObject("headers");
                        String status = jsonHeaders.getString("status");
                        if (!status.equals("OK")) {     // NOT OK
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                                        alertDialog.setTitle(status);
                                        alertDialog.setMessage(jsonHeaders.getString("error_msg") + "\n請聯絡研究人員");
                                        alertDialog.setCanceledOnTouchOutside(false);
                                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "關閉", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                        alertDialog.show();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } else {        // OK
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "上傳完成", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        client.close();
                    } catch (AssertionError e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "連線錯誤，請確認網路是否正常連線", Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("錯誤：沒有資料可上傳");
            alertDialog.setMessage("請聯絡研究人員\n" + String.join("\n", dataMakeupDateList));
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "關閉", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alertDialog.show();
        }
    }
}