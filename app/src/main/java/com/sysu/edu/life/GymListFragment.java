package com.sysu.edu.life;

import static android.text.TextUtils.isEmpty;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;

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
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ItemFieldBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.template.RecyclerAdapter;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class GymListFragment extends Fragment {

    static GymReservationViewModel viewModel;
    HttpManager http;
    Params params;
    StaggeredGridLayoutManager layoutManager;
    RecyclerViewScrollBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
            params = new Params(this);
            params.setCallback(this::getInfo);
            layoutManager = new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL);
            binding.getRoot().setLayoutManager(layoutManager);
            viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);
            FieldAdapter fieldAdapter = new FieldAdapter();
            fieldAdapter.setAction(id -> {
                Bundle bundle = new Bundle();
                bundle.putString("id", id);
                bundle.putInt("code", requireArguments().getInt("code") + 1);
//                viewModel.position.postValue(0);
                Navigation.findNavController(binding.getRoot()).navigate(R.id.campus_to_field, bundle);
            });
            fieldAdapter.setParams(params);
            binding.getRoot().setAdapter(fieldAdapter);
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    String response = (String) msg.obj;
                    System.out.println(response);
                    System.out.println(msg.getData().getInt("code"));
                    switch (msg.getData().getInt("code")) {
                        case 401 -> {
                            params.gotoLogin(binding.getRoot(), viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
//                            viewModel.authorizationManager.setAccessible(false);
//                            http.setAuthorizationRequired(true);
//                            params.toast(R.string.educational_wifi_warning);
//                            getInfo();
                        }
                        case 200 -> {
                            if (!msg.getData().getBoolean("isJSON")) {
                                if (!viewModel.authorizationManager.isAuthorized(response)) {
                                    params.toast(R.string.login_warning);
                                    params.gotoLogin(binding.getRoot(), viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                                    return;
                                }
                                if (Pattern.compile("人机识别检测").matcher(response).find()) {
                                    params.gotoLogin(binding.getRoot(), viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                                    return;
                                }
                                if (!viewModel.authorizationManager.isAccessible(response)) {
                                    params.toast(R.string.educational_wifi_warning);
                                    getInfo();
                                }
                            } else {
                                JSONArray data = JSONArray.parseArray(response);
                                if (data != null && !data.isEmpty()) {
                                    switch (msg.what) {
                                        case 1 ->
                                                data.forEach(e -> fieldAdapter.add((JSONObject) e));
                                        case 2 -> data.forEach(e -> {
                                                    if (Objects.equals(((JSONObject) e).getString("Campus"), requireArguments().getString("id")))
                                                        fieldAdapter.add((JSONObject) e);
                                                }
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            });
            http.setParams(params);
            http.setAuthorizationRequired(true);
            http.setAuthorizationJar(new AuthorizationJar(requireContext()));
            http.setHeader(Map.of("Accept", "application/json, text/plain, */*", "User-Agent", viewModel.ua));
            getInfo();
        }
        return binding.getRoot();
    }

    private void getInfo() {
        if (Objects.equals(requireArguments().getInt("code"), 0)) getCampus();
        else getVenue();
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        layoutManager.setSpanCount(params.getColumn());
    }


    void getCampus() {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + "api/Campus/active", 1);
    }

    void getVenue() {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + "api/venuetype/all", 2);
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

        Consumer<String> action;

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
                Glide.with(holder.itemView.getContext()).load(new GlideUrl(imageUrl, new LazyHeaders.Builder().addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .addHeader("Cookie", CookieManager.getInstance().getCookie(imageUrl))
                        .addHeader("Authorization", params.getAuthorization())
                        .build())).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(binding.image);
        }
    }
}
