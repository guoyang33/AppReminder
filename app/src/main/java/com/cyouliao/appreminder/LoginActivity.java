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

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

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
                    try {
                        RequestParams requestParams = new RequestParams();
                        requestParams.put("exp_id", exp_id);
                        requestParams.put("password", password);
                        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
                        asyncHttpClient.post("http://120.108.111.131/App_3rd/user_login.php", requestParams, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                super.onSuccess(statusCode, headers, response);
                                JSONObject responseHeaders = null;
                                JSONObject responseContents = null;
                                try {
                                    responseHeaders = response.getJSONObject("headers");
                                    String status = responseHeaders.getString("status");
                                    String error_msg = responseHeaders.getString("error_msg");
                                    if (status.equals("OK")) {
                                        responseContents = response.getJSONObject("contents");
                                        JSONObject user_remote = responseContents.getJSONObject("user");

                                        user.setU_id(user_remote.getInt("id"));
                                        user.setExp_id(user_remote.getString("exp_id"));
                                        user.setAddiction(user_remote.getInt("addiction"));
                                        user.setExp_type(user_remote.getString("exp_type"));
                                        user.setPassword((user_remote.getString("password")));
                                        user.update();

                                        Toast.makeText(LoginActivity.this, "登入成功", Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        finish();
                                    } else {        // 登入失敗
                                        // 顯示對話框
                                        AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
                                        alertDialog.setTitle(status);
                                        alertDialog.setMessage(error_msg + "\n請聯絡研究人員");
                                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "關閉", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                        alertDialog.show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } finally { }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                super.onFailure(statusCode, headers, throwable, errorResponse);
                                System.out.println("Request failed: " + statusCode);
                                Toast.makeText(LoginActivity.this, "連線失敗，請檢查網路是否正常連線", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }  finally { }
                }
            }
        });
    }

    public void closeWindow() {
        finish();
    }
}