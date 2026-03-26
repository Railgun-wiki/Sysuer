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
import com.sysu.edu.databinding.FragmentResultBinding;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class AssistantEvaluationResultFragment extends StaggeredFragment {

    HttpManager http;
    int page = 1;
    int total = -1;
    int order = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentResultBinding resultBinding = FragmentResultBinding.inflate(inflater, container, false);
        resultBinding.getRoot().addView(super.onCreateView(inflater, resultBinding.getRoot(), savedInstanceState));
        Params params = new Params(this);
        params.setCallback(() -> {
            reset();
            getResult();
        });
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response.getInteger("code") == 200) {
                        if (msg.what == 0) {
                            JSONObject data = response.getJSONObject("data");
                            if (total == -1) total = data.getInteger("total");
                            data.getJSONArray("rows").forEach(
                                    item -> add(String.valueOf(order++), List.of("学年学期", "助教学期", "助教姓名", "助教培养单位", "教学班号", "课程名称", "课程编码", "课程类别", "课程教学类型", "开课单位", "是否开班", "是否合班", "总教学班号", "任课教师", "课程学时", "助教承担的课程教学学时", "上课时间地点", "助教考核结论")
                                            , extractValue((JSONObject) item, new String[]{"yearTerm", "assistantNum", "assistantName", "assistantCollege", "classNum", "courseName", "courseNum", "courseType", "courseTeachingType", "courseCollege", "openClassFlag", "mergeClassFlag", "sumClassNum", "teacherName", "courseHours", "assistantHours", "teachingTimePlace", "conclusion"}))
                            );
                        }
                    } else {
                        params.toast(response.getString("msg"));
                        params.gotoLogin(TargetUrl.JWXT);
                    }
                }
            }
        });
        http.setParams(params);
        getResult();
        setScrollBottom(() -> {
            if ((page - 1) * 10 < total) getResult();
        });
        return resultBinding.getRoot();
    }

    void reset() {
        page = 1;
        total = -1;
        order = 1;
        clear();
    }

    void getResult() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/assistant-manage/assistantEvaluation/evaluationResultPageList?code=jwxsd_zjpjck",
                String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, requireArguments().getString("params")),
                0);
    }

}