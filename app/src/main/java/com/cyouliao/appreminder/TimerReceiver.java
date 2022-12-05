package com.cyouliao.appreminder;

import android.app.AlertDialog;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

public class TimerReceiver extends BroadcastReceiver {

    private User user;
    private Notify notify;

    private String DATA_MAKEUP_CHANNEL_ID = "DataMakeup";
    private String TODO_CHANNEL_ID = "Todo";

    @Override
    public void onReceive(Context context, Intent intent) {
        user = new User(context);
        notify = new Notify();

        // 取得user status
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
                    if (status.equals("OK")) {     // NOT OK
                        JSONObject contents = jsonResponse.getJSONObject("contents");
                        // 檢查缺少日期
                        JSONArray dataMakeupDateList = contents.getJSONArray("makeup_date");
                        if (dataMakeupDateList.length() > 0) {      // 尚有資料未上傳
                            notify.pushDataMakeup(context);
                        }
                        // 檢查未完成事項
                        JSONArray todoList = contents.getJSONArray("todo_list");
                        if (todoList.length() > 0) {
                            List<String> list = new ArrayList<>();
                            for (int i = 0; i < todoList.length(); i++) {
                                list.add(todoList.getString(i));
                            }
                            if (list.size() > 0) {
                                if (list.contains("high_risk_situation")) {
                                    notify.pushTodo(context, "high_risk_situation");
                                } else if (list.contains("set_goal")) {
                                    notify.pushTodo(context, "set_goal");
                                } else if (list.contains("training_strategy")) {
                                    notify.pushTodo(context, "training_strategy");
                                } else if (list.contains("self_evaluate")) {
                                    notify.pushTodo(context, "self_evaluate");
                                } else if (list.contains("set_goal_makeup")) {
                                    notify.pushTodo(context, "set_goal_makeup");
                                } else if (list.contains("training_strategy_makeup")) {
                                    notify.pushTodo(context, "training_strategy_makeup");
                                }
                            }

                        }
                    }
//                    String responseContent = EntityUtils.toString(httpEntity);
                    client.close();
                } catch (AssertionError e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }            }
        }).start();

    }

//    public void setDataMakeupDateList(List<String> dataMakeupDateList) {
//        this.dataMakeupDateList = dataMakeupDateList;
//    }
//
//    public void setTodoList(List<String> todoList) {
//        this.todoList = todoList;
//    }
//
//    public List<String> getDataMakeupDateList() {
//        return dataMakeupDateList;
//    }
//
//    public List<String> getTodoList() {
//        return todoList;
//    }
}
