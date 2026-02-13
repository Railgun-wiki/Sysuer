package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.view.Pager2Adapter;

import java.util.ArrayList;

public class MajorInfo extends AppCompatActivity {

    HttpManager http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ArrayList<String> categories = new ArrayList<>();
        binding.toolbar.setTitle(R.string.major_info);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Params params = new Params(this);
        params.setCallback(this::getCategory);
        Pager2Adapter adp = new Pager2Adapter(this);
        binding.pager.setAdapter(adp);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(categories.get(position))).attach();
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            if (msg.what == 0) {
                                categories.clear();
                                response.getJSONArray("data").forEach(a -> {
                                    categories.add(((JSONObject) a).getString("dataName"));
                                    Bundle args = new Bundle();
                                    args.putString("code", ((JSONObject) a).getString("dataNumber"));
                                    adp.add(MajorInfoFragment.newInstance(args));
                                });
                            }
                        }
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.getRoot(), TargetUrl.JWXT);
                    }
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/");
        getCategory();
    }

    void getCategory() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=135", 0);
    }
}