package com.sysu.edu.academic;

import static android.text.TextUtils.isEmpty;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.transition.MaterialContainerTransform;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseFilterBinding;

import java.util.ArrayList;
import java.util.HashMap;


public class CourseSelectionFilterFragment extends Fragment {

    HttpManager http;
    HashMap<String, String> filterValue = new HashMap<>();
    HashMap<String, String> filterName = new HashMap<>();
    CourseSelectionViewModel vm;
    FragmentCourseFilterBinding binding;
    Params params;
    NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (binding == null) {
            vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);
            filterValue = vm.getFilterValue();
            filterName = vm.getFilterName();
            binding = FragmentCourseFilterBinding.inflate(inflater, container, false);
            binding.container.setColumnCount(new Params(this).getColumn());
            params = new Params(this);
            params.setCallback(() -> getData(0));
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) {
                        params.toast(getString(R.string.no_wifi_warning));
                        return;
                    }
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null) {
                        if (response.getInteger("code").equals(200)) {
                            JSONArray data = response.getJSONArray("data");
                            if (data != null) {
                                if (msg.what < 4) getData(msg.what + 1);
                                ArrayList<String> items = new ArrayList<>();
                                ArrayList<String> itemCodes = new ArrayList<>();
                                items.add("");
                                itemCodes.add("");
                                data.forEach(a -> {
                                    items.add(((JSONObject) a).getString(new String[]{"campusName", "dataName", "minorName", "dataName", "dataName"}[msg.what]));
                                    itemCodes.add(((JSONObject) a).getString(new String[]{"id", "dataNumber", "sectionNumber", "dataNumber", "dataNumber"}[msg.what]));
                                });
                                MaterialAutoCompleteTextView v = new MaterialAutoCompleteTextView[]{binding.campus, binding.days, binding.sections, binding.languages, binding.special}[msg.what];
                                v.setSimpleItems(items.toArray(new String[]{}));
                                final int a = msg.what;
                                v.setOnItemClickListener((_, _, i, _) -> {
                                    filterValue.put(new String[]{"campus", "day", "section", "language", "special"}[a], itemCodes.get(i));
                                    filterName.put(new String[]{"campus", "day", "section", "language", "special"}[a], items.get(i));
                                });
                            }
                        } else if (response.getInteger("code").equals(50021000)) {
                            params.toast(response.getString("message"));
                        } else if (response.getInteger("code").equals(53000007)) {
                            params.toast(R.string.login_warning);
                            params.gotoLogin(binding.getRoot(), TargetUrl.JWXT);
                        }
                    }
                    super.handleMessage(msg);
                }
            });
            http.setParams(params);
            http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE");
            getData(0);
            load();

        }
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        submit();
                    }
                }
        );
        return binding.getRoot();
    }

    void reset() {
        filterValue.clear();
        filterName.clear();
        vm.setFilterName(filterName);
        vm.setFilterValue(filterValue);
        load();
    }

    void load() {
        filterName = vm.getFilterName();
        filterValue = vm.getFilterValue();
        binding.campus.setText(filterName.getOrDefault("campus", ""), false);
        binding.course.setText(filterName.getOrDefault("course", ""));
        binding.days.setText(filterName.get("day"), false);
        binding.sections.setText(filterName.get("section"), false);
        binding.languages.setText(filterName.get("language"), false);
        binding.special.setText(filterName.getOrDefault("special", ""), false);
        binding.school.setText(filterName.get("school"));
        binding.teacher.setText(filterName.get("teacher"));
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setTransitionName("miniapp");
        navController = Navigation.findNavController(view);
        binding.reset.setOnClickListener(_ -> reset());
        binding.submit.setOnClickListener(_ -> submit());
        MaterialContainerTransform transition = new MaterialContainerTransform();
        transition.setScrimColor(Color.TRANSPARENT);
        transition.setAllContainerColors(requireContext().getColor(com.google.android.material.R.color.design_default_color_surface));
        setSharedElementEnterTransition(transition);
        setSharedElementReturnTransition(transition);
    }



    void submit() {
        vm.setReturnData(parseFilter(getMap()));
        vm.setFilterName(filterName);
        vm.setFilterValue(filterValue);
        navController.navigateUp();
    }

    void getData(int i) {
        http.getRequest(new String[]{"https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=233",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/AcadyeartermSet/minorName?schoolYear=2025-1",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=204",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=387"}[i], i);
    }

    HashMap<String, String> getMap() {
        filterValue.put("course", getEditText(binding.course));
        filterValue.put("teacher", getEditText(binding.teacher));
        filterValue.put("school", getEditText(binding.school));
        filterName.put("course", getEditText(binding.course));
        filterName.put("teacher", getEditText(binding.teacher));
        filterName.put("school", getEditText(binding.school));
        return filterValue;
    }

    String getEditText(EditText editText){
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    String parseFilter(HashMap<String, String> filter) {
        StringBuilder str = new StringBuilder();
        String[] keys = new String[]{"course", "campus", "day", "section", "school", "teacher", "language", "special"};
        String[] key = new String[]{"courseName", "studyCampusId", "week", "classTimes", "courseUnitNum", "teachingTeacherNum", "teachingLanguageCode", "specialClassCode"};
        for (int i = 0; i < keys.length; i++) {
            String v = filter.getOrDefault(keys[i], "");
            if (isEmpty(v)) str.append(String.format(",\"%s\":\"%s\"", key[i], v));
        }
        return str.toString();
    }
}