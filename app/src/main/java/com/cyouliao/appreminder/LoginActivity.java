package com.cyouliao.appreminder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextExp_id;
    private EditText editTextPassword;
    private Button buttonLogin;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        user = new User(getBaseContext());
        editTextExp_id = findViewById(R.id.exp_id_edittext);
        editTextPassword = findViewById(R.id.password_edittext);

        buttonLogin = findViewById(R.id.login_button);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 關閉小鍵盤
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                String exp_id = editTextExp_id.getText().toString();
                String password = editTextPassword.getText().toString();
                if (exp_id.equals("")) {
                    Toast.makeText(getBaseContext(), "請輸入參與者編號", Toast.LENGTH_SHORT).show();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                CloseableHttpClient client = HttpClients.createDefault();
                                HttpPost httpPost = new HttpPost("http://120.108.111.131/App_3rd_2/user_login.php");
                                List<NameValuePair> params = new ArrayList<NameValuePair>();
                                params.add(new BasicNameValuePair("exp_id", exp_id));
                                params.add(new BasicNameValuePair("password", password));
                                httpPost.setEntity(new UrlEncodedFormEntity(params));
                                CloseableHttpResponse response = client.execute(httpPost);
                                assert response.getStatusLine().getStatusCode() == 200;
                                // 轉成JSON Object
                                String responseBody = EntityUtils.toString(response.getEntity());
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                JSONObject jsonHeaders = jsonResponse.getJSONObject("headers");
                                String status = jsonHeaders.getString("status");
                                client.close();
                                if (!status.equals("OK")) {     // NOT OK
                                    // 顯示對話框
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
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
                                    JSONObject jsonContents = jsonResponse.getJSONObject("contents");
                                    JSONObject userRemote = jsonContents.getJSONObject("user");

                                    user.setU_id(userRemote.getInt("id"));
                                    user.setExp_id(userRemote.getString("exp_id"));
                                    user.setAddiction(userRemote.getInt("addiction"));
                                    user.setExp_type(userRemote.getString("exp_type"));
                                    user.setPassword((userRemote.getString("password")));
                                    user.update();

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(LoginActivity.this, "登入成功", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }
                            } catch (AssertionError e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(LoginActivity.this, "連線錯誤，請確認網路是否正常連線", Toast.LENGTH_SHORT).show();
                                    }
                                });
//                                showToast("連線錯誤，請確認網路是否正常連線", Toast.LENGTH_LONG);
//                                Toast.makeText(LoginActivity.this, "連線錯誤，請確認網路是否正常連線", Toast.LENGTH_LONG);
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        });
    }

//    public void showToast(String text, int duration) {
//        Toast.makeText(this, text, duration).show();
//    }
}