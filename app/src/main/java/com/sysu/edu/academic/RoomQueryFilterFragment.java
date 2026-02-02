package com.sysu.edu.academic;

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
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseQueryFilterBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import rikka.material.preference.MaterialSwitchPreference;
import rikka.preference.SimpleMenuPreference;

public class RoomQueryFilterFragment extends PreferenceFragmentCompat {

    HttpManager http;
    FragmentCourseQueryFilterBinding binding;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.room_query_filter, rootKey);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentCourseQueryFilterBinding.inflate(inflater, container, false);
        binding.getRoot().addView(super.onCreateView(inflater, container, savedInstanceState));
        binding.fab.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("params", getParams().toString());
            Navigation.findNavController(binding.getRoot()).navigate(R.id.query_to_result, bundle, new NavOptions.Builder().build());
        });
        Params params = new Params(requireActivity());
        params.setCallback(this, () -> getData(0));
        MaterialSwitchPreference isWeekPreference = Objects.requireNonNull(findPreference("isWeek"));
        SimpleMenuPreference campusPreference = Objects.requireNonNull(findPreference("campus"));
        SimpleMenuPreference buildingPreference = Objects.requireNonNull(findPreference("teachingBuilding"));

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    Integer code = response.getInteger("code");
                    if (code == 200) {
                        ArrayList<String> option = new ArrayList<>();
                        ArrayList<String> number = new ArrayList<>();
                        JSONArray data = response.getJSONArray("data");
                        option.add("");
                        number.add("");
                        if (msg.what < 3) {
                            data.forEach(e -> {
                                JSONObject item = (JSONObject) e;
                                option.add(item.getString(List.of(
                                        "campusName", "name", "acadYearSemester"
                                ).get(msg.what)));
                                number.add(item.getString(List.of(
                                        "id", "id", "acadYearSemester"
                                ).get(msg.what)));
                            });
                            ListPreference preference = Objects.requireNonNull(getPreferenceManager().findPreference(List.of(
                                    "campus", "teachingBuilding", "yearSemester"
                            ).get(msg.what)));
                            preference.setEntries(option.toArray(new String[]{}));
                            preference.setEntryValues(number.toArray(new String[]{}));
                            if (msg.what < 2)
                                getData(msg.what + 1);
                            else
                                getClassRoom(campusPreference, buildingPreference);
                        } else {
                            System.out.println(data);
                            data.forEach(e -> {
                                JSONObject item = (JSONObject) e;
                                option.add(item.getString("number"));
                                number.add(item.getString("id"));
                            });
                            SimpleMenuPreference preference = Objects.requireNonNull(getPreferenceManager().findPreference("classroom"));
                            preference.setEntries(option.toArray(new String[]{}));
                            preference.setEntryValues(number.toArray(new String[]{}));
                        }
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
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
        getData(0);

        isWeekPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isWeek = (boolean) newValue;
            ((PreferenceCategory) Objects.requireNonNull(findPreference("weekSelection"))).setVisible(isWeek);
            ((PreferenceCategory) Objects.requireNonNull(findPreference("dateSelection"))).setVisible(!isWeek);
            return true;
        });
        campusPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getClassRoom(campusPreference, buildingPreference);
            getTeachingBuilding(campusPreference);
            return true;
        });
        buildingPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getClassRoom(campusPreference, buildingPreference);
            return true;
        });
        return binding.getRoot();
    }

    public void getData(int pos) {
        http.getRequest(List.of("https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/teaching-building/pull",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox").get(pos), pos);
    }

    public void getClassRoom(SimpleMenuPreference campusPreference, SimpleMenuPreference buildingPreference) {
        System.out.println(campusPreference.getValue());
        System.out.println(buildingPreference.getValue());
        http.getRequest(String.format("https://jwxt.sysu.edu.cn/jwxt/base-info/classroom/queryclassroombymulticondition?campusId=%s&buildingId=%s",
                campusPreference.getValue() != null ? campusPreference.getValue() : "",
                buildingPreference.getValue() != null ? buildingPreference.getValue() : ""), 3);
    }
    public void getTeachingBuilding(SimpleMenuPreference campusPreference) {
        http.getRequest(String.format("https://jwxt.sysu.edu.cn/jwxt/base-info/teaching-building/pull?campusId=%s",
                campusPreference.getValue() != null ? campusPreference.getValue() : ""), 2);
    }

    public JSONObject getParams() {
        JSONObject params = new JSONObject();
        return params;
    }
}
