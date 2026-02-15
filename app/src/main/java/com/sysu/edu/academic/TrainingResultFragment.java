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
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentTrainingResultBinding;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class TrainingResultFragment extends Fragment {
    HttpManager http;
    int page = 0;
    int total = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentTrainingResultBinding binding = FragmentTrainingResultBinding.inflate(inflater, container, false);
        Params params = new Params(this);
        StaggeredFragment staggeredFragment = new StaggeredFragment();
        params.setCallback(() -> {
            page = 0;
            total = -1;
            staggeredFragment.clear();
            getSelectedCourses();
        });
        getParentFragmentManager().beginTransaction().add(R.id.result, staggeredFragment).commit();
        staggeredFragment.setScrollBottom(() -> {
            if (total > page * 10)
                getSelectedCourses();
        });
        binding.export.setOnClickListener(v -> staggeredFragment.export(v, getString(R.string.result)));
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case -1:
                        params.toast(R.string.no_wifi_warning);
                    case 1:
                        JSONObject response = JSONObject.parse((String) msg.obj);
                        if (response.getIntValue("code") == 200) {
                            JSONObject data = response.getJSONObject("data");
                            total = data.getInteger("total");
                            data.getJSONArray("rows").forEach(o -> staggeredFragment.add(((JSONObject) o).getString("name"), R.drawable.book, List.of("专业", "年级", "学院", "培养类别", "修业年限", "学科门类", "学位", "专业代码", "专业ID"),
                                    extractValue((JSONObject) o, new String[]{"professionName", "grade", "manageUnitName", "trainTypeName", "educationalSystem", "disciplineCateName", "degreeGrantName", "professionCode", "professionId"})));
                        } else {
                            params.toast(response.getString("msg"));
                            params.gotoLogin(binding.getRoot(), TargetUrl.JWXT);
                        }
                        break;
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk");
        getSelectedCourses();
        return binding.getRoot();
    }


    void getSelectedCourses(String unit, String grade, String profession, String trainType) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/training-programe/training-programe/undergradute/profession-info", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":{\"manageUnitNum\":\"%s\",\"grade\":\"%s\",\"professionCode\":\"%s\",\"trainTypeCode\":\"%s\"}}", ++page, unit, grade, profession, trainType), 1);
    }

    void getSelectedCourses() {
        getSelectedCourses(requireArguments().getString("unit"), requireArguments().getString("grade"), requireArguments().getString("profession"), requireArguments().getString("type"));
    }
}