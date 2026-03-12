package com.sysu.edu.life;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityGymPreservationBinding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.regex.Pattern;

public class GymReservationActivity extends AppCompatActivity {
    GymReservationViewModel viewModel;
    CookieManager cookie;
    Params params;
    WebView web;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGymPreservationBinding binding = ActivityGymPreservationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        NavController navController = Objects.requireNonNull(fragment).getNavController();
        NavigationUI.setupWithNavController(binding.toolbar, navController, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
            supportFinishAfterTransition();
            return false;
        }).build());
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        viewModel = new ViewModelProvider(this).get(GymReservationViewModel.class);
        params = new Params(this);
        cookie = CookieManager.getInstance();
        cookie.setAcceptCookie(true);
        initWeb();
        viewModel.loginRequired.observe(this, loginRequired -> {
            if (loginRequired)
                loadUrl(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initWeb() {
        web = new WebView(this);
        WebSettings webSettings = web.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)");
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setJavaScriptEnabled(true);
        webSettings.supportZoom();
        webSettings.setSupportZoom(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        cookie.setAcceptThirdPartyCookies(web, true);
//        ((ViewGroup) requireActivity().findViewById(android.R.id.content)).addView(web);

    }

    private void loadUrl(String link) {
        web.loadUrl(link);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                System.out.println(url);
                if (url.contains(link)) {
//                    handler.postDelayed(() -> {
                    viewModel.cookie = cookie.getCookie(url);
                    web.evaluateJavascript("(function(){return JSON.parse(window.localStorage[\"scientia-session-authorization\"]).access_token;})()", string -> {
                        String authorization;
                        if (!Objects.equals(string, "null") && !Objects.equals(authorization = ("Bearer " + string.replace("\"", "").replace("'", "")), viewModel.authorization.getValue())) {
                            viewModel.authorization.setValue(authorization);
                            if (Boolean.TRUE.equals(viewModel.loginRequired.getValue()))
                                viewModel.loginRequired.setValue(false);
                        }
                    });
//                    },500);
                } else if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/esc-sso/login/page").matcher(url).find()) {
                    web.loadUrl(String.format("""
                                    javascript:(function(){
                                                            function waitElement(selector, callback) {
                                                            const element = document.querySelector(selector);
                                                            if (element) {callback();}else{setTimeout(() => {waitElement(selector,callback);}, 100);}}
                                                            waitElement('.para-widget-account-psw', () => {
                                                            var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click();});})()"""
                            , params.getUserName(), params.getPassword()));
                }
                super.onPageFinished(view, url);
            }
        });
    }

}