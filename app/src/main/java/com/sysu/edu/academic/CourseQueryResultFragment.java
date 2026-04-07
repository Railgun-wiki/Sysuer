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

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseQueryResultBinding;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;

public class CourseQueryResultFragment extends StaggeredFragment {

    HttpManager http;
    int page = 1;
    int total = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentCourseQueryResultBinding courseQueryResultBinding = FragmentCourseQueryResultBinding.inflate(inflater, container, false);
        courseQueryResultBinding.getRoot().addView(super.onCreateView(inflater, courseQueryResultBinding.getRoot(), savedInstanceState), -1, -1);
        params = new Params(this);
        params.setCallback(() -> {
            reset();
            getCourses();
        });
        courseQueryResultBinding.fab.setOnClickListener(_ -> export(courseQueryResultBinding.fab, getString(R.string.course)));
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    Integer code = response.getInteger("code");
                    if (code == 200) {
                        if (total == -1)
                            total = response.getJSONObject("data").getInteger("total");
                        response.getJSONObject("data").getJSONArray("rows").forEach(e -> {
                            JSONObject row = (JSONObject) e;
                            ArrayList<String> values = extractValue(row, new String[]{"yearTerm", "courseName", "courseNum", "openingUnitName", "courseCategoryName", "score", "teachingName", "limitNumber", "selectedNumber", "examMode", "teachingTimePlaceStr", "openingSchoolName", "readObj", "classNumber"});
                            if (values.get(10) != null)
                                values.set(10, values.get(10).replaceAll(",", "\n").replaceAll("/", " | "));
                            add(values.get(1), List.of("学年学期", "课程名称", "课程编号", "开课单位", "课程类别", "学分", "主讲教师", "限选人数", "已选人数", "考试方式", "上课信息", "上课校区", "修读对象", "教学班号"), values);
                        });
                    } else if (code == 53000007) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(TargetUrl.JWXT);
                    } else {
                        params.toast(response.getString("message"));
                    }
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
        setScrollBottom(() -> {
            if ((page - 1) * 10 < total)
                getCourses();
        });
        getCourses();
        return courseQueryResultBinding.getRoot();
    }

    void getCourses() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/schoolOpeningCoursesSchedule/querySchoolOpeningCourses", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, requireArguments().getString("params")), 0);
    }

    public void reset() {
        clear();
        page = 1;
        total = -1;
    }
}