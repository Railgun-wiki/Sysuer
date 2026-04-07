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
import androidx.annotation.Nullable;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentQueryBinding;
import com.sysu.edu.preference.FilterPreference;
import com.sysu.edu.preference.PreferenceUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import rikka.preference.SimpleMenuPreference;

public class AssistantEvaluationQueryFragment extends PreferenceFragmentCompat {
    HttpManager http;
    FragmentQueryBinding binding;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.assisant_evaluation, rootKey);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            binding = FragmentQueryBinding.inflate(inflater, container, false);
            binding.getRoot().addView(super.onCreateView(inflater, binding.getRoot(), null));
            binding.fab.setOnClickListener(v -> {
                Bundle data = new Bundle();
                data.putString("params", getParams().toString());
                Navigation.findNavController(binding.getRoot()).navigate(R.id.assistant_evaluation_result, data,
                        new NavOptions.Builder().setEnterAnim(android.R.anim.fade_in).setExitAnim(android.R.anim.fade_out).build()
                        , new FragmentNavigator.Extras(Map.of(v, "query")));
            });
            Params params = new Params(this);
            params.setCallback(this::getYearTerm);
            FilterPreference unit = Objects.requireNonNull(findPreference("unit"));
            unit.getValueLiveData().observe(requireActivity(), this::getUnit);
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == -1) {
                        params.toast(R.string.no_net_connected);
                    } else {
                        JSONObject response = JSONObject.parseObject((String) msg.obj);
                        if (response.getInteger("code") == 200) {
                            switch (msg.what) {
                                case 0 -> {
                                    String[] years = extractValue(response.getJSONArray("data"), "acadYearSemester").toArray(new String[0]);
                                    SimpleMenuPreference yearTerm = Objects.requireNonNull(findPreference("yearTerm"));
                                    yearTerm.setEntries(years);
                                    yearTerm.setEntryValues(years);
                                    getUnit(unit.getValue());
                                }
                                case 1 -> {
                                    CommonUtil.Tuple2<ArrayList<String>, ArrayList<String>> extractValue = extractValue(response.getJSONArray("data"), "departmentName", "departmentNumber");
                                    unit.setEntries(extractValue.first.toArray(new String[0]));
                                    unit.setEntryValues(extractValue.second.toArray(new String[0]));
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
            http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/zjgl/");
            getYearTerm();
        }
        return binding.getRoot();
    }

    void getYearTerm() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
    }

    void getUnit(String params) {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/department/findCommonDepartmentPull?nameParm=" + params, 1);
    }

    JSONObject getParams() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(this);
        preferenceUtil.insertMenuValue("yearTerm", "yearTerm");
        preferenceUtil.insertEditValue("teacher", "teacherName");
        preferenceUtil.insertEditValue("courseName", "courseName");
        preferenceUtil.insertFilterValue("unit", "openUnitNum");
        System.out.println(preferenceUtil.getParams());
        return preferenceUtil.getParams();
    }
}