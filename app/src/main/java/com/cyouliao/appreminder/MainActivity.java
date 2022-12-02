package com.cyouliao.appreminder;

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

    Button buttonEnter;
    Button buttonDataUpdate;

    private String DATA_MAKEUP_CHANNEL_ID = "DataMakeup";
    private String TODO_CHANNEL_ID = "Todo";
    private List<String> dataMakeupDateList;
    private List<String> todoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonEnter = (Button) findViewById(R.id.enter_button);
        buttonDataUpdate = (Button) findViewById(R.id.data_update_button);

        dataMakeupDateList = new ArrayList<>();
        todoList = new ArrayList<>();

        // 加入channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channelDataMakeup = new NotificationChannel(DATA_MAKEUP_CHANNEL_ID, "DataMakeupChannel", NotificationManager.IMPORTANCE_HIGH);
            NotificationChannel channelTodo = new NotificationChannel(TODO_CHANNEL_ID, "TodoChannel", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager)getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channelDataMakeup);
            notificationManager.createNotificationChannel(channelTodo);
        }

        // 檢查 Usage access 權限
        AppOpsManager appOps = (AppOpsManager) this.getSystemService(this.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), this.getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        if (!granted) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                try {
                    // 開啟 Usage access 權限設定畫面
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("使用記錄權限");
                    alertDialog.setMessage("請將健康上網APP設為允許");
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "設定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            // 強制重開
                            finish();
                        }
                    });
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "無法開啟允許查看使用情況應用界面", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        } else {        // 已獲得usage access權限
            // 取得user資料
            user = new User(getBaseContext());
            // 檢查登入狀態
            if (!user.is_login()) {     // 未登入
                // 顯示登入界面
                Intent intent = new Intent(MainActivity.this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                appUsage = new AppUsage(getBaseContext(), (UsageStatsManager)getSystemService(USAGE_STATS_SERVICE));
                buttonEnter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, WebActivity.class);
                        startActivity(intent);
                    }
                });

                // 手動上傳資料按鈕
                buttonDataUpdate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setButtonDataUpdateEnabled(false);
                        dataUpload();
                    }
                });

                // 設定計時器
                // 取得狀態資訊
                timerGetStatus();
                // 自動上傳資料
                timerDataUpload();
                // 通知
                tiemrNotify();
            }
        }
    }

    public void setButtonDataUpdateEnabled(Boolean enabled) {
        buttonDataUpdate.setEnabled(enabled);
    }

    public List<String> getDataMakeupDateList() {
        return dataMakeupDateList;
    }

    public void setDataMakeupDateList(List<String> dataMakeupDateList) {
        this.dataMakeupDateList = dataMakeupDateList;
        for (String date :dataMakeupDateList) {
            System.out.println(date);
        }
    }

    public List<String> getTodoList() {
        return todoList;
    }

    public void setTodoList(List<String> todoList) {
        this.todoList = todoList;
    }

    public void pushNotify(String type) {
        NotificationCompat.Builder builder = null;
        if (type.equals("data")) {
            System.out.println("data makeup notify");
            Intent intent = new Intent(getBaseContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, 0);
            builder = new NotificationCompat.Builder(MainActivity.this,DATA_MAKEUP_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle("尚有資料未上傳")
                    .setContentText("點此進行手動作業")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManagerCompat
                    = NotificationManagerCompat.from(MainActivity.this);
            notificationManagerCompat.notify(1,builder.build());
        } else {
            Intent intent = new Intent(getBaseContext(), WebActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, 0);
            String title = null;
            String text = null;
            switch (type) {
                case "high_risk_situation":
                    title = "高風險情境評估未完成";
                    text = "請點此進行填寫";
                    break;
                case "set_goal":
                    title = "本週減量目標尚末設定";
                    text = "請點此進行設定";
                    break;
                case "training_strategy":
                    title = "今日尚有認知策略訓練未完成";
                    text = "請點此完成訓練";
                    break;
                case "self_evaluate":
                    title = "您有自我評估單還未完成填寫";
                    text = "請點此進行填寫";
                    break;
                case "set_goal_makeup":
                    title = "您尚有往期減量目標尚末設定";
                    text = "請點此補設定";
                    break;
                case "training_strategy_makeup":
                    title = "您尚有往期認知策略訓練未完成";
                    text = "請點此補訓練";
                    break;
            }
            if (title != null) {
                builder = new NotificationCompat.Builder(MainActivity.this,TODO_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setContentIntent(pendingIntent);

                NotificationManagerCompat notificationManagerCompat
                        = NotificationManagerCompat.from(MainActivity.this);
                notificationManagerCompat.notify(2,builder.build());
            }
        }


    }

    // 推送通知
    private void tiemrNotify() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                List<String> dataMakeupDateList = getDataMakeupDateList();
                List<String> todoList = getTodoList();

                if (dataMakeupDateList.size() > 0) {
                    pushNotify("data");
                }

                if (todoList.size() > 0) {
                    if (todoList.contains("high_risk_situation")) {
                        pushNotify("high_risk_situation");
                    } else if (todoList.contains("set_goal")) {
                        pushNotify("set_goal");
                    } else if (todoList.contains("training_strategy")) {
                        pushNotify("training_strategy");
                    } else if (todoList.contains("self_evaluate")) {
                        pushNotify("self_evaluate");
                    } else if (todoList.contains("set_goal_makeup")) {
                        pushNotify("set_goal_makeup");
                    } else if (todoList.contains("training_strategy_makeup")) {
                        pushNotify("training_strategy_makeup");
                    }
                }
            }
        };

        Timer timer = new Timer(true);
        timer.schedule(timerTask, 0, 30 * 60 * 1000);       // 30分鐘
    }

    // 取得狀態資訊
    private void timerGetStatus() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    CloseableHttpClient client = HttpClients.createDefault();
                    HttpPost httpPost = new HttpPost("http://120.108.111.131/App_3rd/user_status.php");

                    List<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("u_id", "" + user.getU_id()));
                    params.add(new BasicNameValuePair("password", "" + user.getPassword()));
                    httpPost.setEntity(new UrlEncodedFormEntity(params));

                    CloseableHttpResponse response = client.execute(httpPost);
                    assert response.getStatusLine().getStatusCode() == 200;
                    HttpEntity httpEntity = response.getEntity();
                    String responseContent = EntityUtils.toString(httpEntity);
                    System.out.println(responseContent);
                    client.close();
//                    HttpClient httpClient = new Def
//                    String uri = "http://120.108.111.131/App_3rd/user_status.php";
//                    ArrayList<NameValuePair> postParameters;
//                    HttpPost httpPost = new HttpPost(uri);
//                    httpPost.p


//                    AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
//                    asyncHttpClient.post("http://120.108.111.131/App_3rd/user_status.php", requestParams, new JsonHttpResponseHandler() {
//                        @Override
//                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                            super.onSuccess(statusCode, headers, response);
//                            JSONObject responseHeaders = null;
//                            JSONObject responseContents = null;
//                            try {
//                                responseHeaders = response.getJSONObject("headers");
//                                String status = responseHeaders.getString("status");
//                                String error_msg = responseHeaders.getString("error_msg");
//                                if (status.equals("OK")) {     // u_id 不存在 或是 password 不正確
//                                    responseContents = response.getJSONObject("contents");
//                                    // 檢查缺少日期
//                                    JSONArray dataMakeupDateList = responseContents.getJSONArray("makeup_date");
//                                    if (dataMakeupDateList.length() > 0) {
//                                        List<String> dateList = new ArrayList<>();
//                                        for (int i=0; i<dataMakeupDateList.length(); i++) {
//                                            dateList.add(dataMakeupDateList.getString(i));
//                                        }
//                                        setDataMakeupDateList(dateList);
//                                    }
//                                    // 檢查未完成事項
//                                    JSONArray todoList = responseContents.getJSONArray("todo_list");
//                                    List<String> todo = new ArrayList<>();
//                                    for (int i=0; i<todoList.length(); i++) {
//                                        todo.add(todoList.getString(i));
//                                    }
//                                    setTodoList(todo);
//                                } else {        // stauts != OK
//                                }
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            } finally { }
//                        }
//
//                        @Override
//                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
//                            super.onFailure(statusCode, headers, throwable, errorResponse);
//                            System.out.println("Request failed: " + statusCode);
////                                    Toast.makeText(MainActivity.this, "連線失敗，請檢查網路是否正常連線", Toast.LENGTH_SHORT).show();
//                        }
//                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }  finally { }
            }
        };

        Timer timer = new Timer(true);
        timer.schedule(timerTask, 0, 10 * 60 * 1000);       // 10分鐘
    }

    // 自動上傳資料 每天2次 01:00/13:00
    public void timerDataUpload() {
        try {
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 獲取當取小時
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

            long delayTime;
            long currentTime = System.currentTimeMillis();

            if (currentHour < 1) {
                // 當日01點
                // 延時時間 = 當天1點(或13點) - 當時時間
                String currentDay = format1.format(new Date());
                long currentDay01 = format2.parse(currentDay + " " + "01:00:00").getTime();
                delayTime = currentDay01 - currentTime;
            } else if (currentTime < 13) {
                // 當日13點
                String currentDay = format1.format(new Date());
                long currentDay13 = format2.parse(currentDay + " " + "13:00:00").getTime();
                delayTime = currentDay13 - currentTime;
            } else {
                // 隔日01點
                Date date = new Date();
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                c.add(Calendar.DATE, 1);

                String nextDay = format1.format(c.getTime());
                long nextDay01 = format2.parse(nextDay + " " + "01:00:00").getTime();
                delayTime = nextDay01 - currentTime;
            }

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    RequestParams requestParams = new RequestParams();
                    requestParams.put("u_id", user.getU_id());
                    requestParams.put("password", user.getPassword());
                    List<String> dataMakeupDateList = getDataMakeupDateList();
                    int usageCount = 0;

                    if (dataMakeupDateList.size() > 0) {
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
                                    requestParams.add(paramKey, usageTime.toString());
                                    usageCount++;
                                }
                            }
                        }

                        if (usageCount > 0) {
                            SyncHttpClient syncHttpClient = new SyncHttpClient();
                            syncHttpClient.post("http://120.108.111.131/App_3rd/data_upload.php", requestParams, new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    super.onSuccess(statusCode, headers, response);
                                    JSONObject responseHeaders = null;
                                    JSONObject responseContents = null;
                                    try {
                                        responseHeaders = response.getJSONObject("headers");
                                        String status = responseHeaders.getString("status");
                                        String error_msg = responseHeaders.getString("error_msg");
                                        System.out.println(response.toString());
                                        if (status.equals("OK")) {
                                            // 清除dataMakeupDateList
                                            setDataMakeupDateList(new ArrayList<>());
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } finally {
                                    }
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                    super.onFailure(statusCode, headers, throwable, errorResponse);
                                    System.out.println("Request failed: " + statusCode);
                                }
                            });
                        }
                    }
                }
            };

            Timer timer = new Timer(true);
            timer.schedule(timerTask, delayTime, 12 * 60 * 1000);       // 12小時
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void dataUpload() {
        RequestParams requestParams = new RequestParams();
        requestParams.put("u_id", user.getU_id());
        requestParams.put("password", user.getPassword());
        List<String> dataMakeupDateList = getDataMakeupDateList();
        int usageCount = 0;

        if (dataMakeupDateList.size() > 0) {
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
                        requestParams.add(paramKey, usageTime.toString());
                        usageCount++;
                    }
                }
            }

            if (usageCount > 0) {
                SyncHttpClient syncHttpClient = new SyncHttpClient();
                syncHttpClient.post("http://120.108.111.131/App_3rd/data_upload.php", requestParams, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        super.onSuccess(statusCode, headers, response);
                        JSONObject responseHeaders = null;
                        try {
                            responseHeaders = response.getJSONObject("headers");
                            String status = responseHeaders.getString("status");
                            String error_msg = responseHeaders.getString("error_msg");
                            System.out.println(response.toString());
                            if (status.equals("OK")) {
                                // 清除dataMakeupDateList
                                setDataMakeupDateList(new ArrayList<>());
                                Toast.makeText(MainActivity.this, "上傳完成", Toast.LENGTH_LONG).show();
                                setButtonDataUpdateEnabled(true);
                            } else {

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        super.onFailure(statusCode, headers, throwable, errorResponse);
                        System.out.println("Request failed: " + statusCode);
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this, "連線失敗，請檢查網路是否正常連線", Toast.LENGTH_LONG).show();
                        setButtonDataUpdateEnabled(true);
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "沒有資料可上傳", Toast.LENGTH_LONG).show();
                setButtonDataUpdateEnabled(true);
            }
        } else {
            Toast.makeText(MainActivity.this, "目前不需要上傳", Toast.LENGTH_LONG).show();
            setButtonDataUpdateEnabled(true);
        }
    }
}