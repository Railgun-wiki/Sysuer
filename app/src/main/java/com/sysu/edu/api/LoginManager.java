package com.sysu.edu.api;

import static android.text.TextUtils.isEmpty;

import android.util.Log;
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

    private final OkHttpClient directClient = new OkHttpClient.Builder().followRedirects(false).cookieJar(cookieJar).build();
    private final AuthorizationManager casAuthorizationManager = new AuthorizationManager("https://cas.sysu.edu.cn", "https://cas.sysu.edu.cn");
    AuthorizationJar authorizationJar;

    private String getPublicKey() {
        try {
            return client.newCall(new Request.Builder()
                    .url(casAuthorizationManager.getBaseUrl() + "/esc-sso/api/v3/auth/policy").build()).execute().body().string();
        } catch (IOException _) {
        }
        return "";
    }

    private String doLogin(String username, String password, String publicKeyId) {
        try {
            Response response = client.newCall(new Request.Builder()
                    .post(RequestBody.create("{\"authType\":\"webLocalAuth\",\"dataField\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"publicKeyId\":\"" + publicKeyId + "\"}}", MediaType.parse("application/json")))
                    .url("https://cas.sysu.edu.cn/esc-sso/api/v3/auth/doLogin").build()).execute();
            return response.body().string();
        } catch (IOException _) {

        }
        return "";
    }


    private void request(String path) {
        String url = path.startsWith("http") ? path : casAuthorizationManager.getBaseUrl() + path;
        try {
            Response response = client.newCall(new Request.Builder().url(url).build()).execute();
            String a = response.body().string();
            if (Objects.requireNonNull(response.header("Content-Type", "")).contains("application/json"))
                request(redirect(a));
        } catch (IOException _) {
        }
    }

    private String loginForGym(String path) throws IOException {
        String url = path.startsWith("http") ? path : casAuthorizationManager.getBaseUrl() + path;
        Response response = client.newCall(new Request.Builder().header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
                .url(url).build()).execute();
        String a = response.body().string();
        if (Objects.requireNonNull(response.header("Content-Type", "")).contains("application/json")) {
            return loginForGym(redirect(a));
        }
        return a;
    }

    private String redirect(String response) {
        JSONObject json = JSONObject.parse(response);
        if (json.containsKey("data")) return json.getJSONObject("data").getString("redirect");
        else return json.getString("redirect");
    }

    public boolean login(String service) {
        try {
            return authorizationJar != null && login(authorizationJar.getUserName(), authorizationJar.getPassword(), service);
        } catch (ExecutionException | InterruptedException e) {
            Log.e("LoginManager", "login: ", e);
            return false;
        }
    }

    public boolean login(String username, String password, String service) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String host = HttpUrl.get(service).host();
                String targetBaseUrl = HttpUrl.get(service).scheme() + "://" + host + "/";
                if (service.contains("webvpn")) {
                    casAuthorizationManager.setAccessible(false);
                    JSONObject publicKey = JSONObject.parse(getPublicKey()).getJSONObject("data").getJSONObject("param");
                    request(redirect(doLogin(username, encrypt(publicKey.getString("publicKey"), password), publicKey.getString("publicKeyId"))) + "?service=https%3A%2F%2Fwebvpn.sysu.edu.cn%2Fusers%2Fauth%2Fcas%2Fcallback%3Furl");
                    request("https://webvpn.sysu.edu.cn/vpn_key/update");
                    List<Cookie> webvpn = cookieJar.loadForRequest(HttpUrl.get("https://webvpn.sysu.edu.cn/vpn_key/update")).stream().filter(e -> "_webvpn_key".equals(e.name())).collect(Collectors.toList());
                    cookieJar.saveFromResponse(HttpUrl.get(service), webvpn);
                    cookieJar.saveFromResponse(HttpUrl.get(casAuthorizationManager.getBaseUrl()), webvpn);
                    switch (service) {
                        case TargetUrl.NEWS_WEBVPN ->
                                setAuthorization(host, getNewsAuthorization(service));
                        case TargetUrl.GYM_WEBVPN -> {
                            getGymToken(targetBaseUrl);
                            cookieJar.copy(targetBaseUrl, "https://gym.webvpn.sysu.edu.cn");
                            setAuthorization(host, getGymAuthorization(targetBaseUrl));
                        }
                        default -> request("/esc-sso/login?service=" + service);
                    }
                    if (Objects.equals(service, TargetUrl.XGXT_WEBVPN))
                        getXGXTToken(service, targetBaseUrl);
                } else {
                    JSONObject keys = JSONObject.parse(getPublicKey()).getJSONObject("data").getJSONObject("param");
                    request(redirect(doLogin(username, encrypt(keys.getString("publicKey"), password), keys.getString("publicKeyId"))) + "?service=" + service);
                    switch (service) {
                        case TargetUrl.GYM -> {
                            getGymToken(targetBaseUrl);
//                            cookieJar.copy(targetBaseUrl, "https://gym.webvpn.sysu.edu.cn");
                            setAuthorization(host, getGymAuthorization(targetBaseUrl));
                        }
                        case TargetUrl.PORTAL ->
                                request("https://mportal.sysu.edu.cn/newClient/auth?service=https%3A%2F%2Fmportal.sysu.edu.cn%2FnewClient%2F%23%2FnewPortal%2Findex");
                        case TargetUrl.PAY -> {
                            String token = getPayToken(service);
                            setToken(host, token);
                            cookieJar.saveFromResponse(HttpUrl.get(service), List.of(new Cookie.Builder().name("ibps-1.0.1-token").value(token).domain("pay.sysu.edu.cn").build()));
                        }
                        case TargetUrl.ZHNY -> {
                            if (authorizationJar != null)
                                authorizationJar.setAuthorization(host, getZHNYAuthoritarian(service));
                        }
                        case TargetUrl.XGXT -> getXGXTToken(service, targetBaseUrl);
                        case TargetUrl.NEWS ->
                                setAuthorization(host, getNewsAuthorization(service));
                        case TargetUrl.LMS -> setToken(host, getLmsToken());

                    }
                }
                return true;
            } catch (Exception e) {
                Log.e("LoginManager", e.getMessage(), e);
            }
            return false;
        }).get();
    }

    private void setToken(String host, String token) {
        if (authorizationJar != null)
            authorizationJar.setToken(host, token);
    }

    /*
     * 设置认证
     * @param host 主机
     * @param auth 认证
     * */
    private void setAuthorization(String host, String auth) {
        if (authorizationJar != null) authorizationJar.setAuthorization(host, "Bearer " + auth);
    }

    private void getXGXTToken(String service, String targetBaseUrl) throws IOException {
        client.newCall(new Request.Builder().url(targetBaseUrl + "sso/login?realm=sysuRealm&ticket=" + getTicket(service) + "&service=" + service)
                .post(RequestBody.create("", MediaType.parse("application/x-www-form-urlencoded")))
                .build()).execute();
    }

    private String getPayToken(String service) throws IOException {
        return JSONObject.parse(client.newCall(new Request.Builder().url("https://pay.sysu.edu.cn/client/api/client/auth/netId/login")
                .header("Referer", "https://pay.sysu.edu.cn/")
                .post(RequestBody.create("{\"key\":\"https://cas.sysu.edu.cn/cas/serviceValidate?service=https://pay.sysu.edu.cn/sso&ticket=" + getTicket(service) + "\"}", MediaType.parse("application/json")))
                .build()).execute().body().string()).getString("data");
    }

    private String getLmsToken() throws IOException {
        String response = client.newCall(new Request.Builder().url("https://lms.sysu.edu.cn/my/")
                .build()).execute().body().string();
        Matcher matcher = Pattern.compile("\"sesskey\":\"(.+?)\"").matcher(response);
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    private String getZHNYAuthoritarian(String service) throws IOException {
        return JSONObject.parse(client.newCall(new Request.Builder().url("https://zhny.sysu.edu.cn/kbp/auth/third/h5/casLogin/" + getTicket(service))
                .build()).execute().body().string()).getString("data");
    }

    private String encrypt(String publicKeyBase64, String plainText) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64))));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes("UTF-8")));
    }

    public String getTicket(String service) throws IOException {
        String location = directClient.newCall(new Request.Builder().url(casAuthorizationManager.getBaseUrl() + "/esc-sso/login?service=" + service).build()).execute().header("Location");
        return location == null ? "" : HttpUrl.get(location).queryParameter("ticket");
    }

    public void setAuthorization(AuthorizationJar authorizationJar) {
        this.authorizationJar = authorizationJar;
    }

    public void getGymToken(String targetBaseUrl) throws IOException {
        Matcher re = Pattern.compile("prefix = '(.+?)'").matcher(loginForGym(targetBaseUrl));
        String prefix = "";
        List<Cookie> filterChallenge = cookieJar.loadForRequest(HttpUrl.get(targetBaseUrl)).stream().filter(e -> "safeline_bot_challenge".equals(e.name())).collect(Collectors.toCollection(ArrayList::new));
        if (re.find()) prefix = re.group(1);
        if (!filterChallenge.isEmpty() && !isEmpty(prefix))
            cookieJar.saveFromResponse(HttpUrl.get(targetBaseUrl), List.of(new Cookie.Builder().domain(HttpUrl.get(targetBaseUrl).host()).name("safeline_bot_challenge_ans").value(Answer.encode(prefix, filterChallenge.get(0).value())).build()));
    }

    public String getNewsAuthorization(String url) {
        return getAuthorization(new Request.Builder().url(casAuthorizationManager.getBaseUrl() + "/esc-sso/login?service=" + url).build());
    }

    public String getGymAuthorization(String targetBaseUrl) {
        return getAuthorization(new Request.Builder().url(targetBaseUrl + "authsport/Account/Auth?response_type=token&client_id=sysu_2021&redirect_uri=https%3A%2F%2gym.sysu.edu.cn%2F%23&client_id=unnc&scope=PE").header("Accept", "application/json, text/plain, */*")
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
            String host = url.host();
            List<Cookie> currentCookies = _cookieStore.get(host);
            List<Cookie> responseCookies = new ArrayList<>(cookies);
            List<String> keys = responseCookies.stream().map(Cookie::name).collect(Collectors.toCollection(ArrayList::new));
            if (currentCookies != null && !responseCookies.isEmpty()
                    && !currentCookies.isEmpty())
                currentCookies.stream().filter(currentCookie -> !responseCookies.contains(currentCookie) && (!currentCookie.value().isEmpty()) && (!keys.contains(currentCookie.name()))).forEach(responseCookies::add);
            _cookieStore.put(host, responseCookies);
            responseCookies.forEach(e -> cookieManager.setCookie(url.toString(), e.toString()));
        }

        @NonNull
        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = _cookieStore.get(url.host());
            List<Cookie> loginCookies = new ArrayList<>();
            if (cookies != null && !cookies.isEmpty())
                loginCookies = cookies.stream().filter(currentCookie -> !currentCookie.value().isEmpty()).collect(Collectors.toList());
            return loginCookies;
        }

        public void copy(String from, String to) {
            saveFromResponse(HttpUrl.get(to), loadForRequest(HttpUrl.get(from)));
        }

        public String toString(String url) {
            return loadForRequest(HttpUrl.get(url)).stream().map(Cookie::toString).collect(Collectors.joining("; "));
        }
    }

    static class Answer {
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
}