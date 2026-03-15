package com.sysu.edu.life;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityGymPreservationBinding;

import java.util.Objects;

public class GymReservationActivity extends AppCompatActivity {
    GymReservationViewModel viewModel;
    //    CookieManager cookie;
//    Params params;
    WebView web;

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
//        params = new Params(this);
//        cookie = CookieManager.getInstance();
//        cookie.setAcceptCookie(true);
////        initWeb();
//       /* viewModel.loginRequired.observe(this, loginRequired -> {
//            if (loginRequired)
//                loadUrl(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
//        });*/
    }
//
//    @SuppressLint("SetJavaScriptEnabled")
//    public void initWeb() {
//        web = new WebView(this);
//        WebSettings webSettings = web.getSettings();
//        webSettings.setDomStorageEnabled(true);
//        webSettings.setDatabaseEnabled(true);
//        webSettings.setLoadWithOverviewMode(true);
//        webSettings.setUseWideViewPort(true);
//        webSettings.setBlockNetworkImage(false);
//        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)");
//        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.supportZoom();
//        webSettings.setSupportZoom(true);
//        webSettings.setDisplayZoomControls(false);
//        webSettings.setBuiltInZoomControls(false);
//        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
//        cookie.setAcceptThirdPartyCookies(web, true);
////        ((ViewGroup) requireActivity().findViewById(android.R.id.content)).addView(web);
//
//    }
//
//    private void loadUrl(String link) {
//        web.loadUrl(link);
//        web.setWebViewClient(new WebViewClient() {
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                System.out.println(url);
//                if (url.contains(link)) {
////                    handler.postDelayed(() -> {
//                    viewModel.cookie = cookie.getCookie(url);
//                    web.evaluateJavascript("(function(){return JSON.parse(window.localStorage[\"scientia-session-authorization\"]).access_token;})()", string -> {
//                        String authorization;
//                        if (!Objects.equals(string, "null") && !Objects.equals(authorization = ("Bearer " + string.replace("\"", "").replace("'", "")), viewModel.authorization.getValue())) {
//                            viewModel.authorization.setValue(authorization);
//                            if (Boolean.TRUE.equals(viewModel.loginRequired.getValue()))
//                                viewModel.loginRequired.setValue(false);
//                        }
//                    });
////                    },500);
//                } else if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/esc-sso/login/page").matcher(url).find()) {
//                    web.loadUrl(String.format("""
//                                    javascript:(function(){
//                                                            function waitElement(selector, callback) {
//                                                            const element = document.querySelector(selector);
//                                                            if (element) {callback();}else{setTimeout(() => {waitElement(selector,callback);}, 100);}}
//                                                            waitElement('.para-widget-account-psw', () => {
//                                                            var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click();});})()"""
//                            , params.getUserName(), params.getPassword()));
//                }
//                super.onPageFinished(view, url);
//            }
//        });
//    }
//
}