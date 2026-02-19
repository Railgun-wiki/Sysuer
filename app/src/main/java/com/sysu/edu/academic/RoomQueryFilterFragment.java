package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentQueryBinding;
import com.sysu.edu.preference.FilterPreference;
import com.sysu.edu.preference.PreferenceUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import rikka.material.preference.MaterialSwitchPreference;
import rikka.preference.SimpleMenuPreference;

public class RoomQueryFilterFragment extends PreferenceFragmentCompat {

    HttpManager http;
    FragmentQueryBinding binding;
    MaterialDatePicker<Pair<Long, Long>> datePicker;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.room_query_filter, rootKey);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout list = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);
//        if (binding == null) {
        binding = FragmentQueryBinding.inflate(inflater, container, false);
        binding.getRoot().addView(list);
        binding.fab.setOnClickListener(_ -> {
            Bundle bundle = new Bundle();
            bundle.putString("params", getParams().toString());
            Navigation.findNavController(binding.getRoot()).navigate(R.id.query_to_result, bundle, new NavOptions.Builder().build());
        });
        Params params = new Params(this);
        params.setCallback(() -> getData(0));
        MaterialSwitchPreference isWeekPreference = Objects.requireNonNull(findPreference("isWeek"));
        SimpleMenuPreference campusPreference = Objects.requireNonNull(findPreference("campus"));
        SimpleMenuPreference buildingPreference = Objects.requireNonNull(findPreference("teachingBuilding"));
        FilterPreference classroomPreference = Objects.requireNonNull(findPreference("classroom"));
        PreferenceCategory weekSelection = Objects.requireNonNull(findPreference("weekSelection"));
        PreferenceCategory dateSelection = Objects.requireNonNull(findPreference("dateSelection"));
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
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
                        if (msg.what < 4) {
                            data.forEach(e -> {
                                JSONObject item = (JSONObject) e;
                                option.add(item.getString(List.of(
                                        "campusName", "name", "acadYearSemester", "number"
                                ).get(msg.what)));
                                number.add(item.getString(List.of(
                                        "id", "id", "acadYearSemester", "id"
                                ).get(msg.what)));
                            });
                            ListPreference preference1 = Objects.requireNonNull(getPreferenceManager().findPreference(List.of(
                                    "campus", "teachingBuilding", "yearSemester", "classroom"
                            ).get(msg.what)));
                            preference1.setEntries(option.toArray(new String[]{}));
                            preference1.setEntryValues(number.toArray(new String[]{}));
                            if (msg.what < 3)
                                getData(msg.what + 1);
                        } else {
                            data.forEach(e -> {
                                JSONObject item = (JSONObject) e;
                                option.add(item.getString(List.of("name", "number").get(msg.what - 4)));
                                number.add(item.getString(List.of("id", "id").get(msg.what - 4)));
                            });
                            ListPreference preference1 = Objects.requireNonNull(getPreferenceManager().findPreference(List.of(
                                    "teachingBuilding", "classroom"
                            ).get(msg.what - 4)));
                            preference1.setEntries(option.toArray(new String[]{}));
                            preference1.setEntryValues(number.toArray(new String[]{}));
                        }
                    } else if (code == 53000007) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(getView(), TargetUrl.JWXT);
                    } else {
                        params.toast(response.getString("message"));
                    }
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/schedule-web/");
        getData(0);
        isWeekPreference.setOnPreferenceChangeListener((_, newValue) -> {
            boolean isWeek = (boolean) newValue;
            weekSelection.setVisible(isWeek);
            dateSelection.setVisible(!isWeek);
            return true;
        });
        campusPreference.setOnPreferenceChangeListener((_, newValue) -> {
            getTeachingBuilding((String) newValue);
            getClassRoom((String) newValue, buildingPreference.getValue(), classroomPreference.getValueLiveData().getValue());
            return true;
        });
        buildingPreference.setOnPreferenceChangeListener((_, newValue) -> {
            getClassRoom(campusPreference.getValue(), (String) newValue, classroomPreference.getValueLiveData().getValue());
            return true;
        });
        Preference datePreference = Objects.requireNonNull(findPreference("date"));
        datePicker = MaterialDatePicker.Builder.dateRangePicker().build();
        datePicker.addOnPositiveButtonClickListener(_ -> datePreference.setSummary(datePicker.getHeaderText()));
        classroomPreference.getValueLiveData().observe(requireActivity(), value -> getClassRoom(campusPreference.getValue(), buildingPreference.getValue(), value));
        datePreference.setOnPreferenceClickListener(_ -> {
            datePicker.show(getChildFragmentManager(), "date_picker");
            return true;
        });
//        }
        return binding.getRoot();
    }

    public void getData(int pos) {
        http.getRequest(List.of("https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/teaching-building/pull",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/classroom/queryclassroombymulticondition").get(pos), pos);
    }

    public void getTeachingBuilding(String campus) {
        http.getRequest(String.format("https://jwxt.sysu.edu.cn/jwxt/base-info/teaching-building/pull?campusId=%s", campus == null ? "" : campus), 4);
    }

    public void getClassRoom(String campus, String building, String value) {
        http.getRequest(String.format("https://jwxt.sysu.edu.cn/jwxt/base-info/classroom/queryclassroombymulticondition?campusId=%s&buildingId=%s&classroomCode=%s", campus == null ? "" : campus, building == null ? "" : building, value == null ? "" : value), 5);
    }

    /*
     * {"campusId":"5062201","teachingBuildID":"2513856","classroomID":"2514104","sectionA":"1","sectionB":"12","checkType":"2","yearTerm":"2025-1","weekA":"11","weekB":"11","singleOrDoubleWeek":"0","dayWeeks":["日","一","二"],"weekOrTime":"week"}
     * */
    public JSONObject getParams() {
        PreferenceUtil preferenceUtil = new PreferenceUtil(this);
        preferenceUtil.insertMenuValue("campus", "campusId");
        preferenceUtil.insertMenuValue("teachingBuilding", "teachingBuildID");
        preferenceUtil.insertFilterValue("classroom", "classroomID");
        preferenceUtil.insertSliderValue("classBegin", "sectionA");
        preferenceUtil.insertSliderValue("classEnd", "sectionB");
        preferenceUtil.insertMenuValue("checkType", "checkType");
        preferenceUtil.insertMenuValue("occupySource", "occupySource");
        preferenceUtil.insertEditValue("occupyReason", "occupyReason");

        boolean isWeek = ((MaterialSwitchPreference) Objects.requireNonNull(findPreference("isWeek"))).isChecked();
        preferenceUtil.getParams().put("weekOrTime", isWeek ? "week" : "time");
        if (isWeek) {
            preferenceUtil.insertMenuValue("yearSemester", "yearTerm");
            preferenceUtil.insertSliderValue("weekBegin", "weekA");
            preferenceUtil.insertSliderValue("weekEnd", "weekB");
            preferenceUtil.insertMenuValue("weekTime", "singleOrDoubleWeek");
            preferenceUtil.insert("dayWeeks", (((MultiSelectListPreference) Objects.requireNonNull(findPreference("weekdays"))).getValues()));
        } else if (datePicker.getSelection() != null) {
            if (datePicker.getSelection().first != null)
                preferenceUtil.insert("dateA", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(datePicker.getSelection().first)));
            if (datePicker.getSelection().second != null)
                preferenceUtil.insert("dateB", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(datePicker.getSelection().second)));
        }
        return preferenceUtil.getParams();
    }
}
