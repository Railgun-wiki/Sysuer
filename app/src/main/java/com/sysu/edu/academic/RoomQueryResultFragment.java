package com.sysu.edu.academic;

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

import java.util.ArrayList;
import java.util.List;

public class RoomQueryResultFragment extends StaggeredFragment {
    HttpManager http;
    FragmentCourseQueryResultBinding courseQueryResultBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        courseQueryResultBinding = FragmentCourseQueryResultBinding.inflate(inflater, container, false);
        courseQueryResultBinding.getRoot().addView(super.onCreateView(inflater, courseQueryResultBinding.getRoot(), savedInstanceState), -1, -1);
        params = new Params(requireActivity());
        params.setCallback(this, () -> {
            clear();
            getRooms();
        });
        courseQueryResultBinding.fab.setOnClickListener(v -> export(courseQueryResultBinding.fab, getString(R.string.course)));
        Handler handler = new Handler(Looper.getMainLooper()) {
            @NonNull
            private static ArrayList<String> getStrings(JSONObject row) {
                ArrayList<String> values = new ArrayList<>();
                for (String i : new String[]{"yearTerm", "date", "week", "dayWeek", "date", "campus", "teachingBuild", "teachingBuildNum", "classroomNum", "floor", "classroomID", "seatCount"})
                    values.add(row.getString(i));
                for (String i : new String[]{"oneSection", "twoSection", "threeSection", "fourSection", "fiveSection", "sixSection", "sevenSection", "eightSection", "nineSection", "tenSection", "elevenSection", "twelveSection", "thirteenSection", "fourteenSection", "fifteenSection", "sixteenSection"}) {
                    JSONObject section = row.getJSONObject(i);
                    String occupyUseDepartment = section.getString("occupyUseDepartment");
                    String occupyReason = section.getString("occupyReason");
                    values.add((occupyReason == null ? "" : occupyReason) + (occupyUseDepartment == null ? "" : "-" + occupyUseDepartment));
                }
                return values;
            }

            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    Integer code = response.getInteger("code");
                    if (code == 200) {
                        response.getJSONObject("data").getJSONArray("data").forEach(e -> {
                            ArrayList<String> values = getStrings((JSONObject) e);
                            add(values.get(8), List.of(getString(R.string.year_term), getString(R.string.date), getString(R.string.week_range), getString(R.string.week), getString(R.string.date), getString(R.string.campus), getString(R.string.office), getString(R.string.teaching_building_number), getString(R.string.classroom_number), getString(R.string.floor), getString(R.string.classroom_id), getString(R.string.seat_count),
                                    getString(R.string.first_section), getString(R.string.second_section), getString(R.string.third_section), getString(R.string.fourth_section), getString(R.string.fifth_section), getString(R.string.sixth_section), getString(R.string.seventh_section), getString(R.string.eighth_section), getString(R.string.ninth_section), getString(R.string.tenth_section), getString(R.string.eleventh_section), getString(R.string.twelfth_section), getString(R.string.thirteenth_section), getString(R.string.fourteenth_section), getString(R.string.fifteenth_section), getString(R.string.sixteenth_section)), values);
                        });
                    } else if (code == 53000007) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(getView(), TargetUrl.JWXT);
                    } else {
                        params.toast(response.getString("message"));
                    }
                }
            }
        };
        http = new HttpManager(handler);
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/schedule-web/");
        clear();
        getRooms();
        return courseQueryResultBinding.getRoot();
    }

    void getRooms() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/classroomOccupy/pageCheckList", String.format("{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":%s}", requireArguments().getString("params")), 0);
    }

}
