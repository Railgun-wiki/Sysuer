package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;

import java.util.ArrayList;
import java.util.List;

public class DormActivity extends AppCompatActivity {

    HttpManager http;
    AuthorizationManager auth = new AuthorizationManager("https://xgxt.sysu.edu.cn/", "https://xgxt-443.webvpn.sysu.edu.cn/");
    ArrayList<String> tabs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        params.setCallback(this::getDormInfo);
        binding.toolbar.setTitle(R.string.dorm);
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        Pager2Adapter pager2Adapter = new Pager2Adapter(this);
        binding.pager.setAdapter(pager2Adapter);

        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(tabs.get(position))).attach();
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                boolean isJSON = msg.getData().getBoolean("isJSON");
                int code = msg.getData().getInt("code");
                String response = (String) msg.obj;
                if (code == 0) {
                    auth.setAccessible(false);
                    getDormInfo();
                    return;
                }
                if (response == null) {
                    params.toast(R.string.no_wifi_warning);
                    return;
                }
                if (!isJSON) {
                    if (!auth.isAuthorized(response)) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.toolbar, auth.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                        return;
                    }
                    if (!auth.isAccessible(response)) {
                        params.toast(R.string.educational_wifi_warning);
                        getDormInfo();
                        return;
                    }
                }
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject data = JSONObject.parseObject(response);
                    if (data.containsKey("code") && data.getInteger("code") == 200) {
                        data = data.getJSONObject("data");
                        StaggeredFragment list = new StaggeredFragment();
                        tabs.add(getString(R.string.personal_info));
                        pager2Adapter.add(list);
                        list.add(DormActivity.this, getString(R.string.personal_info), List.of(getString(R.string.name), getString(R.string.student_id), getString(R.string.gender), getString(R.string.school), getString(R.string.major), getString(R.string.grade), getString(R.string.training_level), getString(R.string.stay_school_status), getString(R.string.student_status), getString(R.string.contact_number)),
                                separate(data, new String[]{"name", "studentNumber", "gender", "academy", "major", "grade", "trainingLevel", "staySchoolStatus", "studentStatus", "contactNumber"}));

                        StaggeredFragment list1 = new StaggeredFragment();
                        tabs.add(getString(R.string.dorm_info));
                        pager2Adapter.add(list1);
                        data.getJSONArray("stayRecordList").forEach(e -> {
                            ArrayList<String> values = separate((JSONObject) e, new String[]{"schoolYear", "campus", "buildingName", "floorName", "roomNumber", "bedNumber", "accommodationFee", "startDate", "endDate"});
                            list1.add(DormActivity.this, values.get(0), List.of(getString(R.string.year), getString(R.string.campus), getString(R.string.building), getString(R.string.floor), getString(R.string.room_number), getString(R.string.bed_number), getString(R.string.accommodation_fee), getString(R.string.stay_start_date), getString(R.string.stay_end_date)), values);
                        });

                        StaggeredFragment list2 = new StaggeredFragment();
                        tabs.add(getString(R.string.dorm_fee));
                        pager2Adapter.add(list2);
                        data.getJSONArray("stayChargeRecordList").forEach(e -> {
                            ArrayList<String> values = separate((JSONObject) e, new String[]{"schoolYear", "shouldPayStayCharge", "realPayStayCharge", "charge", "arrears"});
                            list2.add(DormActivity.this, values.get(0), List.of(getString(R.string.year), getString(R.string.accommodation_standard), getString(R.string.should_pay_stay_charge), getString(R.string.real_pay_stay_charge), getString(R.string.arrears)), values);
                        });

                    } else if (data.getJSONObject("meta").getInteger("statusCode") == 302) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.toolbar, auth.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                    }
                }
            }
        });
        http.setParams(params);
        getDormInfo();
    }

    void getDormInfo() {
        http.getRequest(auth.getBaseUrl() + "ssgl/api/sm-ssgl/stu-info", 0);
    }

    ArrayList<String> separate(JSONObject data, String[] keys) {
        ArrayList<String> values = new ArrayList<>();
        for (String i : keys) {
            values.add(data.getString(i));
        }
        return values;
    }
}