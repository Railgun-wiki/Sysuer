package com.sysu.edu.life;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.databinding.ItemSchoolBusNoticeBinding;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;

public class SchoolBusActivity extends AppCompatActivity {
    HttpManager http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        final ArrayList<String> routes = new ArrayList<>();
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Params params = new Params(this);
        params.setCallback(this::getData);
        binding.toolbar.setTitle(R.string.school_bus);
        Pager2Adapter adp = new Pager2Adapter(this);
        binding.pager.setAdapter(adp);
        ItemSchoolBusNoticeBinding header = ItemSchoolBusNoticeBinding.inflate(getLayoutInflater(), binding.appBarLayout, false);
        header.day.setOnClickListener(_ -> {
            boolean selected = header.day.isSelected();
            header.day.setText(selected ? R.string.workday : R.string.weekend);
            header.day.setSelected(!selected);
        });
        AlertDialog notice = new MaterialAlertDialogBuilder(this).setTitle(R.string.notice).create();
        header.notice.setOnClickListener(_ -> notice.show());
        header.option.setOnItemClickListener((_, _, position, _) -> {
            binding.pager.setCurrentItem(position);
        });
        binding.appBarLayout.addView(header.getRoot());
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(routes.get(position))).attach();
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else if (!msg.getData().getBoolean("isJSON")) {
                    params.toast(R.string.login_warning);
                    params.gotoLogin(binding.toolbar, TargetUrl.PORTAL);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getJSONObject("meta").getInteger("statusCode").equals(200) && response.get("data") != null) {
                        if (msg.what == 0) {
                            /*holiday*/
                            response.getJSONObject("data").getJSONArray("workDay").forEach(a -> {
                                StaggeredFragment fragment = new StaggeredFragment();
                                JSONObject item = (JSONObject) a;
                                routes.add(item.getString("drivingDirectionName"));
                                ArrayList<String> infos = new ArrayList<>();
                                for (String i : new String[]{"drivingDirectionName", "startStation", "endStation"}) {
                                    infos.add(item.getString(i));
                                }
                                header.option.setSimpleItems(routes.toArray(new String[0]));
                                notice.setMessage(item.getString("note"));
                                fragment.add(SchoolBusActivity.this, getString(R.string.route_detail), List.of("路线", "起点", "终点"), infos);
                                adp.add(fragment);
                                item.getJSONArray("schoolBusShuttleMomentList").forEach(b -> {
                                    ArrayList<String> values = new ArrayList<>();
                                    for (String i : new String[]{"passenger", "vehiclesType", "time", "drivingRoute"})
                                        values.add(((JSONObject) b).getString(i));
                                    fragment.add(SchoolBusActivity.this, values.get(2),  List.of("乘客", "车辆", "时间", "路线"), values);
                                });
                            });
                        }
                    } else {
                        params.toast(getString(R.string.login_warning));
                        params.gotoLogin(binding.toolbar, TargetUrl.PORTAL);
                    }
                }
            }
        });
        http.setParams(params);
        getData();
    }

    void getData() {
        http.getRequest("https://portal.sysu.edu.cn/newClient/api/extraCard/schoolBusShuttleInfo/selectSchoolBusMap", 0);
    }
}