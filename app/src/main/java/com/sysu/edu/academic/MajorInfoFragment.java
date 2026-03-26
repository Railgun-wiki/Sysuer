package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Locale;

public class MajorInfoFragment extends StaggeredFragment {

    int page = 0;
    int total = -1;
    String code;
    HttpManager http;

    public static MajorInfoFragment newInstance(Bundle args) {
        MajorInfoFragment fragment = new MajorInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        code = requireArguments().getString("code");
        Params params = new Params(this);
        params.setCallback(() -> {
            page = 0;
            total = -1;
            clear();
            getList();
        });
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                if (!v.canScrollVertically(1) && total > page * 10) getList();
            }
        });
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            JSONObject data = response.getJSONObject("data");
                            if (msg.what == 0) {
                                if (total == -1) {
                                    total = data.getInteger("total");
                                }
                                data.getJSONArray("rows").forEach(a -> add(((JSONObject) a).getString("name"), List.of("专业代码", "专业名称", "学制", "修业年限", "学科门类", "学位授予门类"),
                                        extractValue((JSONObject) a, new String[]{"code", "name", "educationalSystem", "maxStudyYear", "disciplineCateName", "degreeGrantName"})));
                            }
                        }
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(TargetUrl.JWXT);
                    }
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/");
        getList();
        return view;
    }

    void getList() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/profession-direction/list", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"majorProfessionDircetion\":\"0\",\"disciplineCateCode\":\"%s\"}}", ++page, code), 0);
    }
}