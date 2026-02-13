package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityListBinding;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchoolWorkWarning extends AppCompatActivity {

    HttpManager http;
    ActivityListBinding binding;
    Params params;
    Handler handler;
    String alarmOperationTerm;
    String alarmTerm;
    int order = 0;
    int page = 0;
    int total = -1;
    StaggeredFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        params.setCallback(() -> {
            clear();
            getWarning();
        });
        fragment = binding.list.getFragment();
        fragment.setScrollBottom(() -> {
            if (total > page * 10)
                getWarning();
        });
        fragment.setViewTableMenu(binding.toolbar);
        binding.toolbar.setTitle(R.string.school_work_warning);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        JSONObject data = response.getJSONObject("data");
                        if (data != null) {
                            if (total == -1)
                                total = data.getInteger("total");
                            data.getJSONArray("rows").forEach(a -> {
                                order++;
                                ArrayList<String> values = new ArrayList<>();
                                String[] keyName = new String[]{"预警结果", "预警操作学期", "预警学期", "生成预警档案时间", "档案ID", "警告程度"};
                                for (int i = 0; i < keyName.length; i++) {
                                    values.add(((JSONObject) a).getString(new String[]{"alarmResultName", "alarmOperationTerm", "alarmTerm", "createTime", "archivceID", "alarmResult"}[i]));
                                }
                                fragment.add(SchoolWorkWarning.this, String.valueOf(order), List.of(keyName), values);
                            });
                        }
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
                    }
                }
            }
        };
        http = new HttpManager(handler);
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/");
        getWarning();
    }

    void clear() {
        order = 0;
        page = 0;
        total = -1;
        fragment.clear();
    }

    void getWarning() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/alarm/alarm-archives/student/archives",
                String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"publicationStatus\":\"1\"%s%s}}", ++page, getTerm(alarmTerm), getTerm(alarmOperationTerm)),
                0);
    }

    String getTerm(String s) {
        return (s == null || s.isEmpty()) ? "" : String.format(",\"alarmOperationTerm\":\"%s\"", s);
    }
}