package com.sysu.edu.life;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.trim;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ItemFieldBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.template.RecyclerAdapter;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GymListFragment extends Fragment {

    static GymReservationViewModel viewModel;
    final OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    Params params;
    StaggeredGridLayoutManager layoutManager;
    RecyclerViewScrollBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        if (savedInstanceState == null) {
        binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        params = new Params(this);
        params.setCallback(this::getCampus);
        layoutManager = new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL);
        binding.getRoot().setLayoutManager(layoutManager);
        viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);
        FieldAdapter fieldAdapter = new FieldAdapter(viewModel);
        fieldAdapter.setAction(id -> {
            Bundle bundle = new Bundle();
            bundle.putString("id", id);
            bundle.putInt("code", requireArguments().getInt("code") + 1);
            Navigation.findNavController(binding.getRoot()).navigate(R.id.campus_to_field, bundle);
        });
        viewModel.authorization.observe(getViewLifecycleOwner(), _ -> getInfo());
        viewModel.authorizationManager = new AuthorizationManager("https://gym.sysu.edu.cn/", "https://gym-443.webvpn.sysu.edu.cn/");
        binding.getRoot().setAdapter(fieldAdapter);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle rdata = msg.getData();
                String json = rdata.getString("data");
                JSONArray data;
                if (rdata.getInt("code") == 401) {
                    viewModel.authorizationManager.setAccessible(false);
                    params.toast(R.string.educational_wifi_warning);
                    getInfo();
                } else if (rdata.getInt("code") == 200) {
                    if (!rdata.getBoolean("isJson")) {
                        if (!viewModel.authorizationManager.isAuthorized(json)) {
                            System.out.println("Unauthorized");
                            params.toast(R.string.login_warning);
                            initWeb(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                            return;
                        }
                        if (!viewModel.authorizationManager.isAccessible(json)) {
                            params.toast(R.string.educational_wifi_warning);
                            getInfo();
                            return;
                        }
                        return;
                    }
                    data = JSONArray.parseArray(json);
                    if (data != null) {
                        switch (msg.what) {
                            case 1 -> data.forEach(e -> fieldAdapter.add((JSONObject) e));
                            case 2 -> data.forEach(e -> {
                                        if (Objects.equals(((JSONObject) e).getString("Campus"), requireArguments().getString("id")))
                                            fieldAdapter.add((JSONObject) e);
                                    }
                            );
                        }
                    }
                }
            }
        };
        getInfo();
//        }
        return binding.getRoot();
    }

    private void getInfo() {
        if (Objects.equals(requireArguments().getInt("code"), 0)) {
            getCampus();
        } else {
            getVenue();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initWeb(String link) {
        WebView web = new WebView(requireContext());
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
        CookieManager cookie = CookieManager.getInstance();
        cookie.setAcceptCookie(true);
        cookie.setAcceptThirdPartyCookies(web, true);
        viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.contains(link)) {
//                    handler.postDelayed(() -> {
                    viewModel.token = cookie.getCookie(url);
                    web.evaluateJavascript("(function(){return JSON.parse(window.localStorage[\"scientia-session-authorization\"]).access_token;})()", string -> {
                        String authorization;
                        if (!Objects.equals(string, "null") && !Objects.equals(authorization = ("Bearer " + string.replace("\"", "").replace("'", "")), viewModel.authorization.getValue()))
                            viewModel.authorization.setValue(authorization);
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
        web.loadUrl(link);
//        ((ViewGroup) requireActivity().findViewById(android.R.id.content)).addView(web);

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        layoutManager.setSpanCount(params.getColumn());
    }

    void sendRequest(String url, int what) {
        http.newCall(new Request.Builder()
                .url(url)
                .header("Accept", "application/json, text/plain, */*")
                .header("Cookie", viewModel.token)
                .header("Authorization", Objects.requireNonNull(viewModel.authorization.getValue()))
                .header("User-Agent", viewModel.ua)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = -1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message message = new Message();
                message.what = what;
                Bundle data = new Bundle();
                String dataString = response.body().string();
                data.putInt("code", response.code());
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data", dataString);
                message.setData(data);
                handler.sendMessage(message);
            }
        });
    }

    void getCampus() {
        sendRequest(viewModel.authorizationManager.getBaseUrl() + "api/Campus/active", 1);
    }

    void getVenue() {
        sendRequest(viewModel.authorizationManager.getBaseUrl() + "api/venuetype/all", 2);
    }

    /*public void send() {
        new OkHttpClient().newCall(new Request.Builder()
                        .header("Cookie", viewModel.token)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                        .header("Authorization", Objects.requireNonNull(viewModel.authorization.getValue()))
                        .url("https://gym.sysu.edu.cn/api/Campus/active").build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Matcher prefixMatcher = Pattern.compile("prefix = '(.+)'").matcher(response.body().string());
                        String prefix = null;
                        String safelineBotChallenge = null;
                        if (prefixMatcher.find()) {
                            prefix = prefixMatcher.group(1);
                        }
                        List<String> cookie = response.headers("Set-Cookie");
                        //response.headers().forEach(System.out::println);
                        String cookies = String.join("", cookie);
                        Matcher safelineBotChallengeMatcher = Pattern.compile("safeline_bot_challenge=(.+?);").matcher(cookies);
                        if (safelineBotChallengeMatcher.find()) {
                            safelineBotChallenge = safelineBotChallengeMatcher.group(1);
                        }
                        if (!Pattern.compile("safeline_bot_token=[^0]").matcher(cookies).find()) {
                            viewModel.token = (cookies + "; safeline_bot_challenge_ans=" + encode(prefix, safelineBotChallenge));
                            send();
                        } else {
                            Matcher safelineBotTokenMatcher = Pattern.compile("(safeline_bot_token=.+?);").matcher(cookies);
                            if (safelineBotTokenMatcher.find()) {
                                viewModel.token = cookies;
                                getInfo();
                            }
                        }
                    }
                });

    }*/

    private static class FieldAdapter extends RecyclerAdapter<JSONObject> {

        final GymReservationViewModel viewModel;
        Consumer<String> action;

        public FieldAdapter(GymReservationViewModel viewModel) {
            super();
            this.viewModel = viewModel;
        }

        public void setAction(Consumer<String> action) {
            this.action = action;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemFieldBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemFieldBinding binding = ItemFieldBinding.bind(holder.itemView);
            JSONObject item = get(position);
            binding.title.setText(item.getString("Name"));
            binding.getRoot().setOnClickListener(_ -> action.accept(item.getString("Identity")));
            String imageUrl = item.getString("ImageUrl");
            if (!isEmpty(imageUrl))
                Glide.with(holder.itemView.getContext()).load(new GlideUrl(imageUrl, new LazyHeaders.Builder().addHeader("User-Agent", viewModel.ua)
                        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .addHeader("Cookie", viewModel.token)
                        .addHeader("Authorization", trim(viewModel.authorization.getValue()))
                        .build())).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(binding.image);
        }
    }
}
