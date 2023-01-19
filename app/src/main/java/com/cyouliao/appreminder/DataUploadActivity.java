package com.cyouliao.appreminder;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

public class DataUploadActivity extends AppCompatActivity {

    private User user;
    private AppUsage appUsage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_upload);

        user = new User(this);
        appUsage = new AppUsage(this, (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE));

        // 取得 user status
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
                    client.close();
                    if (!status.equals("OK")) {     // NOT OK
                        AlertDialog alertDialog = new AlertDialog.Builder(DataUploadActivity.this).create();
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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DataUploadActivity.this, "更新成功，正在上傳資料", Toast.LENGTH_LONG).show();
                                }
                            });
                            dataUpload(dateList);
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DataUploadActivity.this, "今日資料已上傳", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                } catch (AssertionError e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DataUploadActivity.this, "連線錯誤，請確認網路是否正常連線", Toast.LENGTH_LONG).show();
                        }
                    });
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        finish();
    }

    public void dataUpload(List<String> dataMakeupDateList) {
        // 類別實例化
        appUsage = new AppUsage(this, (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE));

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("u_id", "" + user.getU_id()));
        params.add(new BasicNameValuePair("password", user.getPassword()));
        int usageCount = 0;
        for (String date : dataMakeupDateList) {
            List<UsageStats> usageStatsList = appUsage.queryUsageStatsByDate(date);
            for (UsageStats usageStats : usageStatsList) {
                Long usageTime = null;
                // 使用時間 api 29以前不能用getTotalTimeVisible()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    usageTime = (usageStats.getTotalTimeVisible() - usageStats.getTotalTimeVisible() % 1000) / 1000;
                } else {
                    usageTime = (usageStats.getTotalTimeInForeground() - usageStats.getTotalTimeInForeground() % 1000) / 1000;
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
                                        AlertDialog alertDialog = new AlertDialog.Builder(DataUploadActivity.this).create();
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
                                    Toast.makeText(DataUploadActivity.this, "上傳完成", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        client.close();
                    } catch (AssertionError e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(DataUploadActivity.this, "連線錯誤，請確認網路是否正常連線", Toast.LENGTH_LONG).show();
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