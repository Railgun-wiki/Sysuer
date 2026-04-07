package com.sysu.edu.studentAffair;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class RecruitmentInfoFragment extends StaggeredFragment {
    final AuthorizationManager auth = new AuthorizationManager("https://xgxt.sysu.edu.cn/", "https://xgxt-443.webvpn.sysu.edu.cn/");
    HttpManager http;
    StudentPartTimeViewModel viewModel;
    Integer total = -1;
    Integer page = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Params params = new Params(this);
        params.setCallback(() -> {
            reset();
            getYear();
        });
        viewModel = new ViewModelProvider(requireActivity()).get(StudentPartTimeViewModel.class);
        viewModel.jobNameDialog.setValueChangeListener(v -> {
            viewModel.jobName.setValue(v);
            reset();
            getRecruitment();
        });
        viewModel.unitDialog.setValueChangeListener(v -> {
            viewModel.unitName.setValue(v);
            reset();
            getRecruitment();
        });
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                boolean isJSON = msg.getData().getBoolean("isJSON");
                int code = msg.getData().getInt("code");
                String response = (String) msg.obj;
                if (code == 0) {
                    auth.setAccessible(false);
                    getYear();
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
                        getYear();
                        return;
                    }
                }
                int what = msg.what;
                if (what == -1) {
                    params.toast(R.string.no_net_connected);
                } else {
                    JSONObject data = JSONObject.parseObject(response);
                    if (data.containsKey("code") && data.getInteger("code") == 200) {
                        switch (what) {
                            case 0 -> {
                                total = data.getJSONObject("data").getInteger("total");
                                data.getJSONObject("data").getJSONArray("list").forEach(i -> add(((JSONObject) i).getString("qgzxgwmc"), List.of("岗位名称", "岗位类型", "所在校区", "岗位地址", "开始时间", "结束时间", "状态", "设岗单位"),
                                        extractValue((JSONObject) i, new String[]{"qgzxgwmc", "qgzxgwlxmc", "qgzxszxymc", "qgzxdwdz", "qgzxgwzpkssj", "qgzxgwzpjssj", "state", "sgdwmc"})));
                            }
                            case 1, 2, 3 -> {
                                MutableLiveData<String> name = List.of(viewModel.yearName, viewModel.campusName, viewModel.jobTypeName).get(what - 1);
                                Menu menu = List.of(viewModel.yearPop, viewModel.campusPop, viewModel.typePop).get(what - 1).getMenu();
                                if (menu.hasVisibleItems()) break;
                                MutableLiveData<String> liveData = List.of(viewModel.year, viewModel.campus, viewModel.jobType).get(what - 1);
                                menu.add(R.string.all).setOnMenuItemClickListener(_ -> {
                                    liveData.setValue("");
                                    name.setValue("");
                                    reset();
                                    getRecruitment();
                                    return true;
                                });
                                data.getJSONArray("data").forEach(i -> menu.add(((JSONObject) i).getString("label")).setOnMenuItemClickListener(_ -> {
                                    liveData.setValue(((JSONObject) i).getString("value"));
                                    name.setValue(((JSONObject) i).getString("label"));
                                    reset();
                                    getRecruitment();
                                    return true;
                                }));
                                new Runnable[]{RecruitmentInfoFragment.this::getCampus, RecruitmentInfoFragment.this::getJobType, RecruitmentInfoFragment.this::getRecruitment}[what - 1].run();
                            }
                        }
                    } else if (data.getJSONObject("meta").getInteger("statusCode") == 302) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(auth.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                    }
                }
            }
        });
        setScrollBottom(() -> {
            if ((page - 1) * 10 < total) getRecruitment();
        });
        http.setParams(params);
        getYear();
        return view;
    }

    private void reset() {
        page = 1;
        total = -1;
        clear();
    }

    void getRecruitment() {
        String url = "qgzx/api/sm-qgzx/gwsq?pageSize=10&pageNum=" + page++;
        if (viewModel.year.getValue() != null && !viewModel.year.getValue().isEmpty())
            url += "&qgzxnd=" + viewModel.year.getValue();
        if (viewModel.jobType.getValue() != null && !viewModel.jobType.getValue().isEmpty())
            url += "&gwlxids=" + viewModel.jobType.getValue();
        if (viewModel.campus.getValue() != null && !viewModel.campus.getValue().isEmpty())
            url += "&xqids=" + viewModel.campus.getValue();
        if (viewModel.jobNameDialog.getValue() != null && !viewModel.jobNameDialog.getValue().isEmpty())
            url += "&qgzxgwmc=" + viewModel.jobNameDialog.getValue();
        if (viewModel.unitDialog.getValue() != null && !viewModel.unitDialog.getValue().isEmpty())
            url += "&sgdwmc=" + viewModel.unitDialog.getValue();
        http.getRequest(auth.getBaseUrl() + url, 0);
    }

    void getYear() {
        http.getRequest(auth.getBaseUrl() + "qgzx/api/sm-qgzx/gwsq/ndlist/get", 1);
    }

    void getCampus() {
        http.getRequest(auth.getBaseUrl() + "qgzx/api/sm-qgzx/gwsq/xylist/get", 2);
    }

    void getJobType() {
        http.getRequest(auth.getBaseUrl() + "qgzx/api/sm-qgzx/gwsq/gwlxlist/get", 3);
    }
}
