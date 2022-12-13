package com.xlzhen.proxywebbrowser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.WebViewClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private AgentWeb agentWeb;
    private String previousUrl;
    private String realUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        realUrl = "https://www.google.com";
        agentWeb = AgentWeb.with(this)
                .setAgentWebParent((LinearLayout) findViewById(R.id.linear_layout), new LinearLayout.LayoutParams(-1, -1))
                .useDefaultIndicator()
                .setWebViewClient(new WebViewClient() {

                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();

                        if (url.startsWith("http")) {
                            if (!url.startsWith(realUrl) && !url.startsWith("http://192.168.5.116:60061")) {
                                previousUrl = realUrl;
                                realUrl = url.substring(0, url.indexOf("/", 9));
                            }
                            url = parseUrl(url);
                            view.loadUrl(url);

                        }
                        return true;
                    }

                    @Override
                    public void onLoadResource(WebView view, String url) {
                        url = parseUrl(url);
                        super.onLoadResource(view, url);
                    }

                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                        WebResourceRequest finalRequest = request;
                        request = new WebResourceRequest() {
                            @Override
                            public Uri getUrl() {
                                String url = parseUrl(finalRequest.getUrl().toString());
                                return Uri.parse(url);
                            }

                            @Override
                            public boolean isForMainFrame() {
                                return finalRequest.isForMainFrame();
                            }

                            @Override
                            public boolean isRedirect() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    return finalRequest.isRedirect();
                                }
                                return false;
                            }

                            @Override
                            public boolean hasGesture() {
                                return finalRequest.hasGesture();
                            }

                            @Override
                            public String getMethod() {
                                return finalRequest.getMethod();
                            }

                            @Override
                            public Map<String, String> getRequestHeaders() {
                                return finalRequest.getRequestHeaders();
                            }
                        };
                        return super.shouldInterceptRequest(view, request);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        setTitle(view.getTitle());
                    }
                })
                .createAgentWeb()
                .ready()
                .go("http://192.168.5.117:60061?host_url=" + realUrl);

    }

    @NonNull
    private String parseUrl(String url) {
        String[] urls = url.split("/");
        url = url.replace(urls[2], "192.168.5.116:60061");
        url = url.replace("https", "http");
        Log.v("url", url);
        if (Uri.parse(url).getQueryParameterNames().size() > 0) {
            url += "&";
        } else {
            url += "?";
        }
        url+=String.format("host_url=%s%s&previousUrl=%s", realUrl, urls[2].equals("192.168.5.116:60061") ? ""
                : String.format("&real_url=%s//%s", urls[0], urls[2]),previousUrl);
        return url;
    }


    @Override
    protected void onPause() {
        agentWeb.getWebLifeCycle().onPause();
        super.onPause();

    }

    @Override
    protected void onResume() {
        agentWeb.getWebLifeCycle().onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        agentWeb.getWebLifeCycle().onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        if (agentWeb.handleKeyEvent(keyCode, event)) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                realUrl = previousUrl;
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}