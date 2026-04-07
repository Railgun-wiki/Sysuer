package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityCourseDetailBinding;
import com.sysu.edu.view.Pager2Adapter;

public class CourseDetailActivity extends AppCompatActivity {

    HttpManager http;
    ActivityCourseDetailBinding binding;
    String code;
    String id;
    String classNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Pager2Adapter courseDetailPageAdapter = new Pager2Adapter(this);
        Params params = new Params(this);
        params.setCallback(this::getDetail);
        binding.pager.setAdapter(courseDetailPageAdapter);
        courseDetailPageAdapter.add(new CourseDetailFragment());
        courseDetailPageAdapter.add(new CourseOutlineFragment());
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, i) -> tab.setText(getString(new int[]{R.string.course_detail, R.string.course_draft}[i]))).attach();
        code = getIntent().getStringExtra("code");
        id = getIntent().getStringExtra("id");
        classNum = getIntent().getStringExtra("class");
        // code: EIT228, id: null, classNum: 202511441
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                System.out.println(msg.obj);
                JSONObject response = JSONObject.parseObject((String) msg.obj);
                if (response.getInteger("code").equals(200)) {
                    switch (msg.what) {
                        case -1 -> params.toast(R.string.no_net_connected);
                        case 1 -> {
                            JSONObject data = response.getJSONObject("data");
                            if (data != null) {
                                Bundle bundle = new Bundle();
                                bundle.putInt("what", 1);
                                bundle.putString("data", data.getJSONObject("outlineInfo").toJSONString());
                                courseDetailPageAdapter.getItem(0).setArguments(bundle);
                                Bundle bundle2 = new Bundle();
                                bundle2.putString("data", data.getJSONArray("scheduleList").toJSONString());
                                courseDetailPageAdapter.getItem(1).setArguments(bundle2);
                                id = data.getJSONObject("outlineInfo").getString("courseId");
                                getCourseOutline2();
                            }
                        }
                        case 2 -> {
                            JSONObject data = response.getJSONObject("data");
                            if (data != null) {
                                Bundle bundle = new Bundle();
                                bundle.putInt("what", 2);
                                bundle.putString("data", data.toString());
                                courseDetailPageAdapter.getItem(0).setArguments(bundle);
                            }
                        }
                    }
                } else if (response.getInteger("code").equals(52000000)) {
                    binding.pager.setVisibility(View.GONE);
                    params.toast(response.getString("message"));
                } else if (response.getInteger("code").equals(53000007)) {
                    params.toast(R.string.login_warning);
                    params.gotoLogin(TargetUrl.JWXT);
                } else {
                    params.toast(response.getString("message"));
                }
                super.handleMessage(msg);
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%25E9%2580%2589%25E8%25AF%25BE");
        getDetail();
    }

    private void getDetail() {
        if (code == null || classNum == null) {
            getCourseOutline2();
        } else {
            getCourseOutline();
        }
    }

    void getCourseOutline() {
        http.getRequest(String.format("https://jwxt.sysu.edu.cn/jwxt/training-programe/courseoutline/getalloutlineinfo?courseNum=%s&auditStatus=99", code), 1);
    }

    void getCourseOutline2() {
        http.getRequest(String.format("https://jwxt.sysu.edu.cn/jwxt/base-info/courseLibrary/findById?id=%s", id), 2);
    }
}