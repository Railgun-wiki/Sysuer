package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.trim;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentTrainingScheduleBinding;

import java.util.ArrayList;
import java.util.Map;

public class TrainingProgramFragment extends Fragment {

    final MutableLiveData<String> unit = new MutableLiveData<>();
    final MutableLiveData<String> profession = new MutableLiveData<>();
    final MutableLiveData<String> type = new MutableLiveData<>();
    final MutableLiveData<String> grade = new MutableLiveData<>();
    FragmentTrainingScheduleBinding binding;
    HttpManager http;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentTrainingScheduleBinding.inflate(inflater);
            binding.unit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    getColleges(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            binding.profession.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    getProfessions(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            binding.query.setOnClickListener(v -> {
                Bundle arg = new Bundle();
                arg.putString("unit", trim(unit.getValue()));
                arg.putString("profession", trim(profession.getValue()));
                arg.putString("grade", trim(grade.getValue()));
                arg.putString("type", trim(type.getValue()));
                Navigation.findNavController(binding.getRoot()).navigate(R.id.confirmationAction,
                        arg, null, new FragmentNavigator.Extras(Map.of(v, "result")));
            });
            Params params = new Params(this);
            params.setCallback(() -> getColleges(""));
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                    } else {
                        JSONObject data = JSONObject.parseObject((String) msg.obj);
                        if (data.getInteger("code") == 200) {
                            deal(msg.what, data, inflater);
                        } else {
                            params.toast(R.string.login_warning);
                            params.gotoLogin(TargetUrl.JWXT);
                        }
                    }
                }
            });
            http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
            http.setParams(params);
            getColleges("");
            getGrades();
            getTypes();
            getProfessions("");
        }
        return binding.getRoot();
    }

    void getProfessions(String keyword) {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/profession-direction/pull?majorProfessionDircetion=1&nameCode=" + keyword, 4);
    }

    void getTypes() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=97", 3);
    }

    void getColleges(String keyword) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/department/recruitUnitPull", "{\"departmentName\":\"" + keyword + "\",\"subordinateDepartmentNumber\":null,\"id\":null}", 1);
    }

    void getGrades() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=127", 2);
    }

    void deal(int what, JSONObject data, LayoutInflater inflater) {
        switch (what) {
            case 1: {
                ArrayList<String> list = new ArrayList<>();
                ArrayList<String> unitIDs = new ArrayList<>();
                data.getJSONArray("data").forEach(e -> {
                    unitIDs.add(((JSONObject) e).getString("departmentNumber"));
                    list.add(((JSONObject) e).getString("departmentName"));
                });
                binding.unit.setSimpleItems(list.toArray(new String[]{}));
                if (binding.unit.hasFocus()) binding.unit.showDropDown();
                binding.unit.setOnItemClickListener((_, _, position, _) -> unit.setValue(unitIDs.get(position)));
                break;
            } // 处理学院
            case 2: {
                ArrayList<String> list = new ArrayList<>();
                ArrayList<String> professionIDs = new ArrayList<>();
                data.getJSONArray("data").forEach(e -> {
                    professionIDs.add(((JSONObject) e).getString("dataNumber"));
                    list.add(((JSONObject) e).getString("dataName"));
                });
                binding.grade.setMinValue(1);
                binding.grade.setMaxValue(list.size());
                binding.grade.setDisplayedValues(list.toArray(new String[0]));
                binding.grade.setOnValueChangedListener((_, _, fromUser) -> grade.setValue(professionIDs.get(fromUser - 1)));
                binding.grade.setValue(list.size());
                grade.postValue(professionIDs.get(list.size() - 1));
                break;
            } // 处理年级
            case 3: {
                data.getJSONArray("data").forEach(e -> {
                    Chip chip = (Chip) inflater.inflate(R.layout.item_filter_chip, binding.types, false);
                    chip.setOnCheckedChangeListener((_, isChecked) -> {
                        if (isChecked) {
                            type.setValue(((JSONObject) e).getString("dataNumber"));
                        }
                    });
                    chip.setText(((JSONObject) e).getString("dataName"));
                    binding.types.addView(chip);
                });
                ((Chip) binding.types.getChildAt(0)).setChecked(true);
                break;
            } // 处理类型
            case 4: {
                ArrayList<String> list = new ArrayList<>();
                ArrayList<String> professionIDs = new ArrayList<>();
                data.getJSONArray("data").forEach(e -> {
                    professionIDs.add(((JSONObject) e).getString("code"));
                    list.add(((JSONObject) e).getString("name"));
                });
                binding.profession.setSimpleItems(list.toArray(new String[]{}));
                if (binding.profession.hasFocus()) binding.profession.showDropDown();
                binding.profession.setOnItemClickListener((_, _, position, _) ->
                        profession.setValue(professionIDs.get(position)));
                break; // 处理专业
            }
        }
    }
}