package com.sysu.edu.login;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginManager {

    static int time = 0;

    /**
     * 初始化登录模型
     * @param activity 活动上下文
     * @param model    登录视图模型
     * @param target   目标登录网址
     * @param afterLogin 登录成功后的回调
     */
    public static void initLoginModel(FragmentActivity activity, LoginViewModel model, String target, Runnable afterLogin) {
        Params params = new Params(activity);
        model.getPassword().observe(activity, params::setPassword);
        model.getAccount().observe(activity, params::setUserName);
        model.setAccount(params.getUserName());
        model.setPassword(params.getPassword());
        model.setTarget(target);
        model.setUrl(TargetUrl.LOGIN);
        model.getLogin().observe(activity, b -> {
            if (b) {
                SharedPreferences.Editor edit = params.getSharedPreferences().edit();
                String cookie = model.getCookie().getValue();
                Matcher match = Pattern.compile("ibps-1.0.1-token=(.+?);").matcher(cookie + ";");
                Matcher authorization = Pattern.compile("authorization=(.+?);").matcher(cookie + ";");
                if (match.find()) edit.putString("token", match.group(1));
                if (authorization.find())
                    edit.putString("authorization", Objects.requireNonNull(authorization.group(1)).replace("%20", " "));
                edit.putString("Cookie", cookie);
                edit.apply();
                afterLogin.run();
            }
        });
    }
    /**
     * 初始化登录WebView
     * @param activity 活动上下文
     * @param model    登录视图模型
     * @param afterLoad 登录完成后的回调
     * @return 初始化好的WebView
     */
    public static WebView initLoginWebView(@NonNull FragmentActivity activity, LoginViewModel model, Runnable afterLoad) {
        WebView web = new WebView(activity);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(web, true);
        cookieManager.acceptThirdPartyCookies(web);
        Handler handler = new Handler(Looper.getMainLooper());
        model.getUrl().observe(activity, web::loadUrl);
        time = 0;
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                time++;
                if (time >= 100) {
                    web.stopLoading();
                    time = 0;
                    return;
                }
                //boolean reloadCap = Objects.equals(sessionId, CookieManager.getInstance().getCookie(url));
                //model.setSessionID(CookieManager.getInstance().getCookie(url));
                System.out.println(url);
                if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/selfcare").matcher(url).find()) {
                    view.loadUrl(Objects.requireNonNull(model.getTarget().getValue()));
                    return;
                }
                if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/esc-sso/login/page").matcher(url).find()) {
                    //System.out.println("登录中");
                    model.setLogin(false);
                    if (afterLoad != null) afterLoad.run();
                    return;
                }
                String element = "";
                if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/#/login").matcher(url).find()) {
                    element = ".ant-btn.ant-btn-primary.ant-btn-block";
                } else if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/#/student").matcher(url).find()) {
                    element = ".ant-btn.ant-btn-primary";
                } else if (Pattern.compile("//pay.sysu.edu.cn/#/$").matcher(url).find()) {
                    element = ".el-button.login_btns.btn-netIdLogin.el-button--default.is-plain";
                } else if (Pattern.compile("//pjxt.sysu.edu.cn/").matcher(url).find()) {
                    element = ".log-g-iddl";
                } else if (Pattern.compile("portal.sysu.edu.cn/newClient/#/login").matcher(url).find()) {
                    element = ".ant-btn.index-submit-3jXSy.ant-btn-primary.ant-btn-lg";
                } else if (Pattern.compile("tice.sysu.edu.cn").matcher(url).find()) {
                    element = "#netid-login";
                }
                if (element.isEmpty()) {
                    if (Pattern.compile(Objects.requireNonNull(model.getTarget().getValue())).matcher(url).find())
                        handler.postDelayed(() -> {
                            model.setCookie(CookieManager.getInstance().getCookie(url));
                            model.setLogin(true);
                        }, 500);
                } else {
                    web.evaluateJavascript("(function(){var needLogin = document.querySelector('" + element + "');if(needLogin!=null){needLogin.click();};return needLogin!=null;})()", s -> {
                        System.out.println(s);
                        if (!Boolean.parseBoolean(s)) {
                            model.setCookie(CookieManager.getInstance().getCookie(url));
                            model.setLogin(true);
                        }
                    });
                }
            }
        });
        initWeb(web);
        return web;
    }

    /**
     * 初始化WebView设置
     * @param web 要初始化设置的WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void initWeb(WebView web) {
        WebSettings webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0");
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDefaultTextEncodingName("utf-8");
    }
}