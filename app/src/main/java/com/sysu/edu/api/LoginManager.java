package com.sysu.edu.api;

import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.crypto.Cipher;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginManager {

    private static final int TIMEOUT = 30;
    private final CookieStore cookieJar = new CookieStore();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(true)
            .cookieJar(cookieJar)
            .build();
    private final AuthorizationManager authorizationManager = new AuthorizationManager("https://cas.sysu.edu.cn", "https://cas-443.webvpn.sysu.edu.cn");
    private String ticket;

    public LoginManager() {
    }


    private String getPublicKey() {
        try {
            return client.newCall(new Request.Builder()
                    .url(authorizationManager.getBaseUrl() + "/esc-sso/api/v3/auth/policy").build()).execute().body().string();
        } catch (IOException _) {
        }
        return "";
    }

    private String doLogin(String username, String password, String publicKeyId) {
        try {
            Response response = client.newCall(new Request.Builder()
                    .post(RequestBody.create("{\"authType\":\"webLocalAuth\",\"dataField\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"publicKeyId\":\"" + publicKeyId + "\"}}", MediaType.parse("application/json")))
                    .url(authorizationManager.getBaseUrl() + "/esc-sso/api/v3/auth/doLogin").build()).execute();
            String string = response.body().string();
            System.out.println(string);
            return string;
        } catch (IOException _) {

        }
        return "";
    }


    private void login(String path) {
//        System.out.println("Cookie " + cookieJar.loadForRequest(HttpUrl.get("https://cas.sysu.edu.cn")));
        String url = path.startsWith("http") ? path : authorizationManager.getBaseUrl() + path;
        try {
            System.out.println(url);
            Response response = client.newCall(new Request.Builder().url(url).build()).execute();
            String a = response.body().string();
            System.out.println(response.headers());
            System.out.println(a);
            String location;
            if (response.priorResponse() != null && (location = response.priorResponse().header("location")) != null
                    && location.contains("ticket")) {
                System.out.println(HttpUrl.get(location).queryParameter("ticket"));
                ticket = HttpUrl.get(location).queryParameter("ticket");
            }
            boolean isJson = Objects.requireNonNull(response.header("Content-Type", "")).contains("application/json");
            if (isJson) {
//                System.out.println("redirect to " + redirect(a));
                login(redirect(a));
            }
        } catch (IOException _) {
        }
    }

    private String redirect(String response) {
        JSONObject json = JSONObject.parse(response);
        if (json.containsKey("data")) return json.getJSONObject("data").getString("redirect");
        else return json.getString("redirect");
    }

    public boolean login(String username, String password, String service) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (service.contains("webvpn")) {
                    authorizationManager.setAccessible(false);
                    JSONObject params = JSONObject.parse(getPublicKey()).getJSONObject("data").getJSONObject("param");
                    login(redirect(doLogin(username, encrypt(params.getString("publicKey"), password), params.getString("publicKeyId"))) + "?service=" + "https%3A%2F%2Fwebvpn.sysu.edu.cn%2Fusers%2Fauth%2Fcas%2Fcallback%3Furl");
                    login("https://webvpn.sysu.edu.cn/vpn_key/update");
                    List<Cookie> webvpn = cookieJar.loadForRequest(HttpUrl.get("https://webvpn.sysu.edu.cn/vpn_key/update")).stream().filter(e -> "_webvpn_key".equals(e.name())).collect(Collectors.toList());
                    cookieJar.saveFromResponse(HttpUrl.get(service), webvpn);
                    cookieJar.saveFromResponse(HttpUrl.get("https://cas-443.webvpn.sysu.edu.cn"), webvpn);
                    login("/esc-sso/login?service=" + service);

                    if (Objects.equals(service, TargetUrl.XGXT_WEBVPN))
                        client.newCall(new Request.Builder().url("https://xgxt-443.webvpn.sysu.edu.cn/sso/login?realm=sysuRealm&ticket=" + ticket + "&service=" + service)
                                .post(RequestBody.create("", MediaType.parse("application/x-www-form-urlencoded")))
                                .build()).execute();
                    return true;
                }
                JSONObject params = JSONObject.parse(getPublicKey()).getJSONObject("data").getJSONObject("param");
                login(redirect(doLogin(username, encrypt(params.getString("publicKey"), password), params.getString("publicKeyId"))) + "?service=" + service);
                if (Objects.equals(service, TargetUrl.PORTAL)) {
                    login("https://mportal.sysu.edu.cn/newClient/auth?service=https%3A%2F%2Fmportal.sysu.edu.cn%2FnewClient%2F%23%2FnewPortal%2Findex");
                }

//                login("https://netpay.sysu.edu.cn/netpay/index.jsp");
//                System.out.println(cookieJar.loadForRequest(HttpUrl.get("https://pay.sysu.edu.cn")));
                System.out.println(cookieJar.loadForRequest(HttpUrl.get("https://xgxt-443.webvpn.sysu.edu.cn/")));
                return true;
            } catch (Exception _) {
            }
            return false;
        }).get();
    }

    private String encrypt(String publicKeyBase64, String plainText) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64))));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes("UTF-8")));
    }

    static class CookieStore implements CookieJar {

        private final CookieManager cookieManager = CookieManager.getInstance();

        private final HashMap<String, List<Cookie>> _cookieStore = new HashMap<>();

        public CookieStore() {
            cookieManager.setAcceptCookie(true);
        }

        @Override
        public void saveFromResponse(HttpUrl url, @NonNull List<Cookie> cookies) {
//            String add = url.queryParameter("service");
            String host = url.host();//add == null ? url.host() : HttpUrl.get(add).host();
            List<Cookie> currentCookies = _cookieStore.get(host);
            List<Cookie> responseCookies = new ArrayList<>(cookies);
            List<String> keys = responseCookies.stream().map(Cookie::name).collect(Collectors.toList());
            if (currentCookies != null && !responseCookies.isEmpty()
                    && !currentCookies.isEmpty())
                currentCookies.stream().filter(currentCookie -> !responseCookies.contains(currentCookie) && (!currentCookie.value().isEmpty()) && (!keys.contains(currentCookie.name()))).forEach(responseCookies::add);
            _cookieStore.put(host, responseCookies);
            responseCookies.forEach(e -> cookieManager.setCookie(url.toString(), e.toString()));
//            System.out.println("add " + url.toString() + " to " + responseCookies);
//            cookieManager.setCookie(url.scheme() + "://" + url.host(), responseCookies.stream().map(Cookie::toString).collect(Collectors.joining(";")), value -> System.out.println("setCookie " + url.scheme() + "://" + url.host() + " " + value));
//            System.out.println("saveFromResponse " + url + " " + responseCookies);
        }

        @NonNull
        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = _cookieStore.get(url.host());
            List<Cookie> requestCookies = new ArrayList<>();
            if (cookies != null && !cookies.isEmpty())
                requestCookies = cookies.stream().filter(currentCookie -> !currentCookie.value().isEmpty()).collect(Collectors.toList());
            return requestCookies;
        }

        public CookieManager getCookieManager() {
            return cookieManager;
        }
    }
}

