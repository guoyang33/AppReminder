package com.cyouliao.appreminder;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class WebActivity extends AppCompatActivity {

    private User user;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        user = new User(getBaseContext());
        webView = (WebView) findViewById(R.id.webview_redirect);
        webView.getSettings().setJavaScriptEnabled(true);
//        String urlRedirect = "http://120.108.111.131/App_3rd/test_user_index.php";
        String urlRedirect = "http://120.108.111.131/App_3rd/redirect.php?u_id=" + user.getU_id();
        webView.loadUrl(urlRedirect);
        webView.setWebViewClient(new WebViewClient() { });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }
}