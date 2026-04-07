package com.sysu.edu.studentAffair;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class CVFragment extends StaggeredFragment {
    final AuthorizationManager auth = new AuthorizationManager("https://xgxt.sysu.edu.cn/", "https://xgxt-443.webvpn.sysu.edu.cn/");
    HttpManager http;
    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = super.onCreateView(inflater, container, savedInstanceState);
            Params params = new Params(this);
            params.setCallback(this::getCV);

            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    boolean isJSON = msg.getData().getBoolean("isJSON");
                    int code = msg.getData().getInt("code");
                    String response = (String) msg.obj;
                    if (code == 0) {
                        auth.setAccessible(false);
                        getCV();
                        return;
                    }
                    if (response == null) {
                        params.toast(R.string.no_net_connected);
                        return;
                    }
                    if (!isJSON) {
                        if (!auth.isAuthorized(response)) {
                            params.toast(R.string.login_warning);
                            params.gotoLogin(auth.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                            return;
                        }
                        if (!auth.isAccessible(response)) {
                            params.toast(R.string.educational_wifi_warning);
                            getCV();
                            return;
                        }
                    }
                    switch (msg.what) {
                        case -1 -> params.toast(R.string.no_net_connected);
                        case 0 -> {
                            JSONObject data = JSONObject.parseObject(response);
                            if (data.containsKey("code") && data.getInteger("code") == 200) {
                                data = data.getJSONObject("data");
                                add(getString(R.string.cv), List.of("学号", "姓名", "培养单位", "专业", "培养层次", "电话号码", "邮箱", "最后修改时间", "家庭人均月收入(元)", "在校每月平均消费(元)", "爱好特长", "勤工助学经历", /*"", */"工作时间", "性别", "住宿地址"),
                                        extractValue(data, new String[]{"xh", "xm", "pydw", "zymc", "pycc", "dhhm", "email", "zhxgsj", "jtrjysr", "zxmypjxf", "ahtc", "qgzxjls",/*"kqgzxsjs",*/"gzsjs", "xb", "ssdz"}));
                                data.getJSONArray("hjqks").forEach(i -> add(getString(R.string.award), List.of("颁奖单位", "颁奖日期", "奖项"),
                                        extractValue((JSONObject) i, new String[]{"bjdw", "bjrq", "jxmc"})));
                                data.getJSONArray("rzjls").forEach(i -> add(getString(R.string.experience), List.of("工作单位", "工作开始年月", "工作结束年月", "工作职务", "证明人", "证明人单位"),
                                        extractValue((JSONObject) i, new String[]{"gzdw", "gzksny", "gzjsny", "gzzw", "zmr", "zmrdwhzw"})));
                            } else if (data.getJSONObject("meta").getInteger("statusCode") == 302) {
                                params.toast(R.string.login_warning);
                                params.gotoLogin(auth.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                            }
                        }
                    }
                }
            });
            http.setParams(params);
            getCV();
        }
        return view;
    }

    void getCV() {
        http.getRequest(auth.getBaseUrl() + "qgzx/api/sm-qgzx/xsjl/get", 0);
    }
}
