package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.haibin.calendarview.Calendar;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentWaterFeeBinding;
import com.sysu.edu.todo.info.TitleAdapter;
import com.sysu.edu.view.FeeMonthView;
import com.sysu.edu.view.FeeWeekView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EnergyWaterFeeFragment extends Fragment {

    HttpManager http;
    String name = "";
    LinkedList<Runnable> request = new LinkedList<>();
    ArraySet<CommonUtil.Tuple2<String, String>> rooms = new ArraySet<>();
    MutableLiveData<String> roomCode = new MutableLiveData<>();
    private ConcatAdapter adapter;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Params params = new Params(this);
        ContextUtil contextUtil = new ContextUtil(requireContext());
        adapter = new ConcatAdapter();

        FragmentWaterFeeBinding binding = FragmentWaterFeeBinding.inflate(inflater, container, false);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.calendarView.setMonthView(FeeMonthView.class);
        binding.calendarView.setWeekView(FeeWeekView.class);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1)
                    params.toast(R.string.no_wifi_warning);
                else if (msg.getData().getBoolean("isJSON")) {
                    JSONObject response = JSONObject.parse((String) msg.obj);
                    if (response.getInteger("code") == 200) {
                        switch (msg.what) {
                            case 0 -> name = response.getJSONObject("data").getString("username");
                            case 1 -> {
                                ArrayAdapter<Object> items = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
                                binding.spinner.setAdapter(items);
                                response.getJSONArray("data").forEach(e -> {
                                    JSONObject roomInfo = (JSONObject) e;
                                    rooms.add(new CommonUtil.Tuple2<>(roomInfo.getString("roomName"), roomInfo.getString("roomCode")));
                                    items.add(roomInfo.getString("roomName"));
                                });
                            }
                            case 2 -> {
                                GymAccountFragment.PreferenceAdapter preferenceAdapter = new GymAccountFragment.PreferenceAdapter();
                                response.getJSONObject("data").getJSONArray("waterUsageList").forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    Object totalWaterUsage = item.get("totalWaterUsage");
                                    String content = totalWaterUsage == null ? getString(R.string.no_data_available) : totalWaterUsage.toString();
                                    Calendar calendar = new Calendar();
                                    calendar.setScheme(content);
                                    calendar.setYear(binding.calendarView.getSelectedCalendar().getYear());
                                    calendar.setMonth(binding.calendarView.getSelectedCalendar().getMonth());
                                    calendar.setDay(Integer.parseInt(item.getString("timeLabel")));
                                    binding.calendarView.addSchemeDate(calendar);
                                });
                                adapter.addAdapter(preferenceAdapter);
                            }
                            case 3 -> {
                                reset();
                                response.getJSONObject("data").getJSONArray("billList").forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    String duration = String.format("%s~%s", item.getString("originalBillStartDate"), item.getString("originalBillEndDate"));
                                    adapter.addAdapter(new TitleAdapter(duration));
                                    GymAccountFragment.PreferenceAdapter preferenceAdapter = new GymAccountFragment.PreferenceAdapter();
                                    ArrayList<String> value = extractValue(item, new String[]{"billStartDate", "paymentStatus", "useWaterTypeName", "finalWaterUsage", "waterPayment", "paidPayment"});
                                    Integer paymentStatus = item.getInteger("paymentStatus");
                                    value.set(0, duration);
                                    value.set(1, new String[]{"待缴费", "缴费中", "已缴费", "缴费失败", "无需缴费"}[paymentStatus - 1]);
                                    preferenceAdapter.set(List.of(R.string.bill_period, R.string.status, R.string.type, R.string.electricity_consumption, R.string.fee, R.string.paid_fee),
                                            value,
                                            List.of(R.drawable.calendar, paymentStatus == 3 || paymentStatus == 5 ? R.drawable.check : R.drawable.uncheck, R.drawable.water, R.drawable.water, R.drawable.money, R.drawable.money), requireContext());
                                    preferenceAdapter.setHideNull(true);
                                    adapter.addAdapter(preferenceAdapter);
                                });
                            }
                        }
                        nextRequest();
                    } else contextUtil.login(TargetUrl.ZHNY, () -> nextRequest());
                }
                super.handleMessage(msg);
            }
        });
        http.setAuthorizationRequired(true);
        http.setAuthorizationJar(new AuthorizationJar(requireContext()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        request.add(this::getUserInfo);
        request.add(() -> getRoom(name));
        request.add(() -> {
            if (!rooms.isEmpty())
                roomCode.setValue(rooms.valueAt(0).getSecond());
        });
        roomCode.observe(getViewLifecycleOwner(), v -> {
            if (v != null) {
                getWaterConsumption(v, LocalDate.of(binding.calendarView.getSelectedCalendar().getYear(), binding.calendarView.getSelectedCalendar().getMonth(), 1).format(formatter));
                getWaterBill(v);
            }
        });
        binding.calendarView.setOnMonthChangeListener((year, month) -> {
            getWaterConsumption(roomCode.getValue(), LocalDate.of(year, month, 1).format(formatter));
            binding.date.setText(LocalDate.of(year, month, 1).format(formatter));
        });
        binding.date.setText(LocalDate.now().format(formatter));
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reset();
                roomCode.setValue(rooms.valueAt(position).getSecond());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        nextRequest();

        return binding.getRoot();
    }

    void nextRequest() {
        if (!request.isEmpty())
            request.pop().run();
    }

    void reset() {
        adapter.getAdapters().forEach(adapter::removeAdapter);
    }

    void getUserInfo() {
        http.getRequest("https://zhny.sysu.edu.cn/kbp/auth/userInfo", 0);
    }

    void getRoom(String username) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/admin/sys/personRoom/list", "{\"username\":\"" + username + "\"}", 1);
    }

    void getWaterConsumption(String room, String date) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/cwbs/month/usage/stats", String.format("{\"roomCode\":\"%s\",\"staticsMonth\":\"%s\"}", room, date), 2);
    }

    void getWaterBill(String room) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/cwbs/mobile/room/bill/list", String.format("{\"roomCode\":\"%s\"}", room), 3);
    }
}
