package com.sysu.edu.life;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.academic.BrowserActivity;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityNewsBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewsActivity extends AppCompatActivity {
    final OkHttpClient http = new OkHttpClient.Builder().build();
    ActivityNewsBinding binding;
    Handler handler;
    Params params;
    AuthorizationManager authorizationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        authorizationManager = new AuthorizationManager("https://iportal.sysu.edu.cn/", "https://iportal-443.webvpn.sysu.edu.cn/");
        class Adapter extends FragmentStateAdapter {
            public Adapter(@NonNull FragmentActivity fragmentActivity) {
                super(fragmentActivity);
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return new NewsFragment(position);
            }

            @Override
            public int getItemCount() {
                return 4;
            }
        }
        Adapter adapter = new Adapter(this);
        binding.pager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabLayout, binding.pager, (tab, position) -> tab.setText(new String[]{"资讯", "公众号", "通知", "今日中大"}[position])).attach();
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
            }
        };
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //adapter.getItem(binding.pager.getCurrentItem()).run.run();
            }
        });
        SuggestionAdapter suggestionAdapter = new SuggestionAdapter();
        params = new Params(this);
        params.setCallback(this::getSuggestions);
        binding.sugs.setAdapter(suggestionAdapter);
        binding.sugs.setLayoutManager(new GridLayoutManager(this, 1));
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle rdata = msg.getData();
                boolean isJson = rdata.getBoolean("isJson");
                String json = rdata.getString("data");
                if (json == null) {
                    params.toast(R.string.no_wifi_warning);
                    return;
                }
                System.out.println(json);
                if (!isJson) {
                    if (!authorizationManager.isAuthorized(json)) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.searchView, authorizationManager.isAccessible() ? TargetUrl.NEWS : TargetUrl.NEWS_WEBVPN);
                        return;
                    }
                    if (!authorizationManager.isAccessible(json)) {
                        params.toast(R.string.educational_wifi_warning);
                        getSuggestions();
                        return;
                    }

                }
                JSONObject data = Objects.requireNonNull(JSONObject.parseObject(json));
                Object code = data.get("code");
                if (Objects.equals(code, "0000")) {
                    if (msg.what == 1) {
                        suggestionAdapter.clear();
                        data.getJSONObject("data").getJSONArray("suggests").forEach(e -> suggestionAdapter.add((String) e));
                    } else if (Objects.equals(code, 496)) {
                        params.toast(data.getString("message"));
                        params.gotoLogin(binding.searchView, authorizationManager.isAccessible() ? TargetUrl.NEWS : TargetUrl.NEWS_WEBVPN);
                    }
                    //suggestion
                } else {
                    params.toast(data.getString("code") + "\n" + data.getString("message"));
                }
            }

        };
        EditText edit = binding.searchView.getEditText();
        edit.setOnEditorActionListener((textView, i, keyEvent) -> {
            binding.searchView.hide();
            return false;
        });
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!edit.getText().toString().isEmpty()) {
                    getSuggestions(edit.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    void getSuggestions() {
        getSuggestions(binding.searchView.getEditText().getText().toString());
    }

    void getSuggestions(String keyword) {
        http.newCall(new Request.Builder().url(authorizationManager.getBaseUrl() + "ai_service/search-server/needle/suggest")
                .post(RequestBody.create(String.format("{\"aliasName\":\"collection_data\",\"keyWord\":\"%s\"}", keyword), MediaType.parse("application/json")))
                .header("Authorization", params.getAuthorization())
                .header("Cookie", params.getCookie()).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                Bundle data = new Bundle();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data", response.body().string());
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }
}

class SuggestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final ArrayList<String> data = new ArrayList<>();

    public SuggestionAdapter() {
        super();
    }

    public void add(String a) {
        int tmp = getItemCount();
        data.add(a);
        notifyItemInserted(tmp);
    }

    public void clear() {
        int tmp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0, tmp);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sug, parent, false);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((TextView) holder.itemView).setText(data.get(position));
        Context context = holder.itemView.getContext();
        holder.itemView.setOnClickListener(v -> context.startActivity(new Intent(context, BrowserActivity.class).setData(Uri.parse(String.format("https://iportal.sysu.edu.cn/searchWeb/#/index?searchWord=%s&module=default&size=10&current=1&sortType=score&searchType=3", data.get(position)))), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context, v, "miniapp").toBundle()));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}