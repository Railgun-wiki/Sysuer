package com.sysu.edu.api;

import static android.text.TextUtils.isEmpty;

import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    ContextUtil params;

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
            return response.body().string();
        } catch (IOException _) {

        }
        return "";
    }


    private void login(String path) {
//        System.out.println("Cookie " + cookieJar.loadForRequest(HttpUrl.get("https://cas.sysu.edu.cn")));
        String url = path.startsWith("http") ? path : authorizationManager.getBaseUrl() + path;
        try {
//            System.out.println(url);
            Response response = client.newCall(new Request.Builder().url(url).build()).execute();
            String a = response.body().string();
//            System.out.println(response.headers());
//            String location;
//            if (response.priorResponse() != null && (location = response.priorResponse().header("location")) != null
//                    && location.contains("ticket")) {
//                System.out.println(HttpUrl.get(location).queryParameter("ticket"));
//                ticket = HttpUrl.get(location).queryParameter("ticket");
//            }

            if (Objects.requireNonNull(response.header("Content-Type", "")).contains("application/json")) {
//                System.out.println("redirect to " + redirect(a));
//                System.out.println(a);
                login(redirect(a));
            }
        } catch (IOException _) {
        }
    }

    private String loginForGym(String path) {
//        System.out.println("Cookie " + cookieJar.loadForRequest(HttpUrl.get("https://cas.sysu.edu.cn")));
        String url = path.startsWith("http") ? path : authorizationManager.getBaseUrl() + path;
        try {
//            System.out.println(url);
            Response response = client.newCall(new Request.Builder().header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
                    .url(url).build()).execute();
            String a = response.body().string();
//            System.out.println(response.headers());
//            String location;
//            if (response.priorResponse() != null && (location = response.priorResponse().header("location")) != null
//                    && location.contains("ticket")) {
//                System.out.println(HttpUrl.get(location).queryParameter("ticket"));
//                ticket = HttpUrl.get(location).queryParameter("ticket");
//            }

            if (Objects.requireNonNull(response.header("Content-Type", "")).contains("application/json")) {
//                System.out.println("redirect to " + redirect(a));
//                System.out.println(a);
                loginForGym(redirect(a));
            }
            return a;
        } catch (IOException _) {
        }
        return "";
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
                    JSONObject publicKey = JSONObject.parse(getPublicKey()).getJSONObject("data").getJSONObject("param");
                    login(redirect(doLogin(username, encrypt(publicKey.getString("publicKey"), password), publicKey.getString("publicKeyId"))) + "?service=https%3A%2F%2Fwebvpn.sysu.edu.cn%2Fusers%2Fauth%2Fcas%2Fcallback%3Furl");
                    login("https://webvpn.sysu.edu.cn/vpn_key/update");
                    List<Cookie> webvpn = cookieJar.loadForRequest(HttpUrl.get("https://webvpn.sysu.edu.cn/vpn_key/update")).stream().filter(e -> "_webvpn_key".equals(e.name())).collect(Collectors.toList());
                    cookieJar.saveFromResponse(HttpUrl.get(service), webvpn);
                    cookieJar.saveFromResponse(HttpUrl.get("https://cas-443.webvpn.sysu.edu.cn"), webvpn);

                    if (Objects.equals(service, TargetUrl.NEWS_WEBVPN)) {
                        if (params != null) {
                            String auth = "Bearer " + getNewsAuthorization(service);
//                            System.out.println(auth);
                            params.setAuthorization(auth);
                        }
                    } else if (Objects.equals(service, TargetUrl.GYM_WEBVPN)) {
                        getToken();
                        cookieJar.copy("https://gym-443.webvpn.sysu.edu.cn", "https://gym.webvpn.sysu.edu.cn");
                        String auth = getGymAuthorization();
//                        login("https://gym-443.webvpn.sysu.edu.cn/authsport/Account/Auth?response_type=token&client_id=sysu_2021&redirect_uri=https%3A%2F%2gym.sysu.edu.cn%2F%23&client_id=unnc&scope=PE");
                        System.out.println(auth);
                        if (params != null)
                            params.setAuthorization("Bearer " + auth);
                    } else {
                        login("/esc-sso/login?service=" + service);
                    }
                    if (Objects.equals(service, TargetUrl.XGXT_WEBVPN))
                        client.newCall(new Request.Builder().url("https://xgxt-443.webvpn.sysu.edu.cn/sso/login?realm=sysuRealm&ticket=" + getTicket(service) + "&service=" + service)
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
//                System.out.println(cookieJar.loadForRequest(HttpUrl.get("https://xgxt-443.webvpn.sysu.edu.cn/")));
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

    public String getTicket(String service) throws IOException {
        String location = new OkHttpClient.Builder().followRedirects(false).cookieJar(cookieJar).build().newCall(new Request.Builder().url(authorizationManager.getBaseUrl() + "/esc-sso/login?service=" + service).build()).execute().header("Location");
        return location == null ? "" : HttpUrl.get(location).queryParameter("ticket");
    }

    public void setContextUtil(ContextUtil params) {
        this.params = params;
    }

    public void getToken() {
        Matcher re = Pattern.compile("prefix = '(.+?)'").matcher(loginForGym("https://gym-443.webvpn.sysu.edu.cn/"));
        String prefix = "";
        // System.out.println(b.stream().filter(e -> "safeline_bot_challenge".equals(e.name())).collect(Collectors.toList()));
        List<Cookie> filterChallenge = cookieJar.loadForRequest(HttpUrl.get("https://gym-443.webvpn.sysu.edu.cn/")).stream().filter(e -> "safeline_bot_challenge".equals(e.name())).collect(Collectors.toList());
        if (re.find()) prefix = re.group(1);
        if (!filterChallenge.isEmpty() && !isEmpty(prefix))
            cookieJar.saveFromResponse(HttpUrl.get("https://gym-443.webvpn.sysu.edu.cn"), List.of(new Cookie.Builder().domain("gym-443.webvpn.sysu.edu.cn").name("safeline_bot_challenge_ans").value(Answer.encode(prefix, filterChallenge.get(0).value())).build()));
    }

    public String getNewsAuthorization(String url) {
        return getAuthorization(new Request.Builder().url(authorizationManager.getBaseUrl() + "/esc-sso/login?service=" + url).build());
    }

    public String getGymAuthorization() {
        return getAuthorization(new Request.Builder().url("https://gym-443.webvpn.sysu.edu.cn/authsport/Account/Auth?response_type=token&client_id=sysu_2021&redirect_uri=https%3A%2F%2gym.sysu.edu.cn%2F%23&client_id=unnc&scope=PE").header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36").build());
    }


    public String getAuthorization(Request request) {
        try {
            Response response = client.newCall(request).execute();
            String location;
            if (response.priorResponse() != null && (location = response.priorResponse().header("location")) != null && location.contains("access_token")) {
                Matcher matcher = Pattern.compile("access_token=(.*?)&").matcher(location);
                if (matcher.find())
                    return matcher.group(1);
            }
        } catch (IOException _) {
        }
        return "";
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

        public void copy(String from, String to) {
            saveFromResponse(HttpUrl.get(to), loadForRequest(HttpUrl.get(from)));
        }

        public String toString(String url) {
            return loadForRequest(HttpUrl.get(url)).stream().map(Cookie::toString).collect(Collectors.joining("; "));
        }

//
//        public CookieManager getCookieManager() {
//            return cookieManager;
//        }
    }
}

class Answer {
    /**
     * 计算字符串的SHA1哈希值，返回十六进制字符串
     */
    public static String hexSha1(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(input.getBytes());
        return bytesToHex(hash);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 将十六进制字符串转换为二进制字符串 每个十六进制字符转换为4位二进制
     */
    public static String hexToBinary(String hexStr) {
        StringBuilder binaryStr = new StringBuilder();
        for (int i = 0; i < hexStr.length(); i++) {
            char c = hexStr.charAt(i);
            int value = Character.digit(c, 16);
            // 转换为4位二进制，前面补0
            String binary = String.format("%4s", Integer.toBinaryString(value)).replace(' ', '0');
            binaryStr.append(binary);
        }
        return binaryStr.toString();
    }

    /**
     * 模拟JS中的bin_sha1函数
     */
    public static String binSha1(String input) throws NoSuchAlgorithmException {
        String hexHash = hexSha1(input);
        return hexToBinary(hexHash);
    }

    /**
     * 找到满足条件的suffix
     */
    public static String findSuffix(String prefix, int leadingZeroBit) throws NoSuchAlgorithmException {
        int cnt = 0;
        while (true) {
            String suffix = Integer.toHexString(cnt);
            String hashBinary = binSha1(prefix + suffix);
            // 检查前leadingZeroBit位是否全为0
            if (hashBinary.substring(0, leadingZeroBit).equals("0".repeat(leadingZeroBit))) {
                return suffix;
            }
            cnt++;
        }
    }

    /**
     * 计算最终的safeline_bot_challenge_ans cookie值
     */
    public static String getFinalCookie(String safelineBotChallenge, String prefix, int leadingZeroBit)
            throws NoSuchAlgorithmException {
        return safelineBotChallenge + findSuffix(prefix, leadingZeroBit);
    }

    public static String encode(String prefix, String safelineBotChallenge) {
        try {
            return getFinalCookie(safelineBotChallenge, prefix, 9);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-1算法不可用: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("发生错误: " + e.getMessage());
        }
        return "";
    }
}
