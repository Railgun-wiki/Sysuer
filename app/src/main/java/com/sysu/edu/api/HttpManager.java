package com.sysu.edu.api;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpManager {
    final OkHttpClient http;
    Handler handler;
    String referrer;
    String cookie;
    String authorization;
    Params params;
    String ua;
    String target;
    boolean isAuthorizationRequired;
    boolean isTokenRequired;
    final CookieManager cookieManager = CookieManager.getInstance();

    public HttpManager(Handler handler) {
        http = new OkHttpClient();
        setHandler(handler);
    }

    public void setParams(Params params) {
        this.params = params;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public void setAuthorizationRequired(boolean isAuthorizationRequired) {
        this.isAuthorizationRequired = isAuthorizationRequired;
    }

    public void setTokenRequired(boolean isTokenRequired) {
        this.isTokenRequired = isTokenRequired;
    }

    public void setUA(String ua) {
        this.ua = ua;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    private void sendRequest(@NonNull String url, String data, String type, int what) {
        if (handler == null) {
            Log.e("HttpManager", "Handler Requested");
            return;
        }
        Request.Builder request = new Request.Builder()
                .url(url);
//        if (params != null) request.header("Cookie", params.getCookie());
        if (cookie != null) request.header("Cookie", cookie);
        if (target != null && cookieManager.getCookie(target) != null) {
            request.header("Cookie", cookieManager.getCookie(target));
        } else if (cookieManager.getCookie(url) != null) {
            request.header("Cookie", cookieManager.getCookie(url));
        }
        if (isAuthorizationRequired && params != null)
            request.header("Authorization", params.getAuthorization());
        if (authorization != null) request.header("Authorization", authorization);
        if (referrer != null) request.header("Referer", referrer);
        if (ua != null) request.header("User-Agent", ua);
        if (data != null) request.post(RequestBody.create(data, MediaType.get(type)));
        if (isTokenRequired && params != null) request.header("token", params.getToken());
        http.newCall(request.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.sendEmptyMessage(-1);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                msg.obj = response.body().string();
                Bundle bundle = new Bundle();
                bundle.putInt("code", response.code());
                bundle.putBoolean("isJSON", Objects.requireNonNull(response.header("Content-Type")).contains("application/json"));
                bundle.putString("data", (String) msg.obj);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        });
    }

    public void postRequest(@NonNull String url, String data, int what) {
        sendRequest(url, data, "application/json", what);
    }

    public void postRequest(@NonNull String url, String data, String type, int what) {
        sendRequest(url, data, type, what);
    }

    public void getRequest(@NonNull String url, int what) {
        sendRequest(url, null, null, what);
    }
}
