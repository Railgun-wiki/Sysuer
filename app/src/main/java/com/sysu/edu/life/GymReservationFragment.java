package com.sysu.edu.life;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentGymOrderBinding;
import com.sysu.edu.todo.info.TitleAdapter;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.ButtonAdapter;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class GymReservationFragment extends Fragment {
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    HttpManager http;
    GymReservationViewModel viewModel;
    private ConcatAdapter concatAdapter;
    private FragmentGymOrderBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGymOrderBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);

        concatAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build());
        binding.recyclerView.setAdapter(concatAdapter);
        Params params = new Params(this);
        params.setCallback(this::reset);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                    // 处理错误
                } else {
                    String response = (String) msg.obj;
                    if (msg.getData().getBoolean("isJSON")) {
                        switch (msg.what) {
                            case 0 -> /*
                             response = """
                                        [{
                                                "Identity": "e9157771-796d-43ca-b36c-d7f12989c407",
                                                "Name": "xx",
                                                "BookingId": "#RB-ASLIGUX4F1",
                                                "UserId": "xx",
                                                "HostKey": "xx",
                                                "VenueTypeId": "802fbfda-f9f6-41c8-9c72-685247f07c73",
                                                "VenueId": "d2c4c59a-1d00-4c44-9a6a-24f26390227e",
                                                "VenueName": "东校园游泳池",
                                                "StartDateTime": "2026-03-18T11:30:00Z",
                                                "EndDateTime": "2026-03-18T13:00:00Z",
                                                "Participants": [],
                                                "Status": "Accepted",
                                                "Description": "东校园游泳池",
                                                "CreatedAt": "2026-03-17T13:38:33.311Z",
                                                "UpdatedAt": "2026-03-17T13:38:33.164Z",
                                                "ActionedBy": "xx",
                                                "Charge": 5,
                                                "IsCash": false
                                            }]
                                        """;*/ JSONArray.parseArray(response).forEach((i) -> {
                                            JSONObject item = (JSONObject) i;
                                            GymAccountFragment.PreferenceAdapter preferenceAdapter = new GymAccountFragment.PreferenceAdapter();
                                            TitleAdapter titleAdapter = new TitleAdapter(item.getString("Description"));
                                            titleAdapter.setHeader(1);
                                            ButtonAdapter buttonAdapter = new ButtonAdapter();
                                            buttonAdapter.add(getString(R.string.cancel_reservation));
                                            buttonAdapter.setListener(new AdapterListener() {
                                                @Override
                                                public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                                                }

                                                @Override
                                                public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
                                                    binding.getRoot().setOnClickListener(_ -> deleteReservation(item.getString("Identity")));
                                                }
                                            });
                                            concatAdapter.addAdapter(titleAdapter);
                                            concatAdapter.addAdapter(preferenceAdapter);
                                            concatAdapter.addAdapter(buttonAdapter);
                                            preferenceAdapter.set(List.of(getString(R.string.venue), getString(R.string.start_time), getString(R.string.end_time), getString(R.string.money)),
                                                    extractValue(item, new String[]{"VenueName", "StartDateTime", "EndDateTime", "Charge"}),
                                                    List.of(R.drawable.location, R.drawable.time, R.drawable.alarm, R.drawable.money));
                                            preferenceAdapter.add(getString(R.string.pay_way), item.getBoolean("IsCash") ? getString(R.string.cash) : getString(R.string.pe_credit), R.drawable.money);

                                        });
                            case 1 -> regetReservation();
                        }
                        /*{
        "Identity": "e9157771-796d-43ca-b36c-d7f12989c407",
        "Name": "xx",
        "BookingId": "#RB-ASLIGUX4F1",
        "UserId": "xx",
        "HostKey": "xx",
        "VenueTypeId": "802fbfda-f9f6-41c8-9c72-685247f07c73",
        "VenueId": "d2c4c59a-1d00-4c44-9a6a-24f26390227e",
        "VenueName": "东校园游泳池",
        "StartDateTime": "2026-03-18T11:30:00Z",
        "EndDateTime": "2026-03-18T13:00:00Z",
        "Participants": [],
        "Status": "Accepted",
        "Description": "东校园游泳池",
        "CreatedAt": "2026-03-17T13:38:33.311Z",
        "UpdatedAt": "2026-03-17T13:38:33.164Z",
        "ActionedBy": "xx",
        "Charge": 5,
        "IsCash": false
    }*/
                    } else {
                        if (!viewModel.authorizationManager.isAuthorized(response)) {
                            params.toast(R.string.login_warning);
//                            viewModel.loginRequired.setValue(true);
                            params.gotoLogin(binding.getRoot(), viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                        } else if (Pattern.compile("人机识别检测").matcher(response).find()) {
                            params.gotoLogin(binding.getRoot(), viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                        } else if (!viewModel.authorizationManager.isAccessible(response)) {
                            params.toast(R.string.educational_wifi_warning);
                            http.setAuthorizationRequired(true);
                            getReservation();
                        }
                    }
                }
            }
        });
        http.setParams(params);
        http.setHeader(Map.of("Accept", "application/json, text/plain, */*"));
//        http.setCookie(viewModel.cookie);
        http.setUA(viewModel.ua);
        http.setAuthorizationRequired(!viewModel.authorizationManager.isAccessible());
        MaterialDatePicker.Builder<Long> picker = MaterialDatePicker.Builder.datePicker();
        binding.from.setOnClickListener(_ -> {
            MaterialDatePicker<Long> datePicker = picker
                    .setSelection(viewModel.reservationFrom)
                    .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointBackward.before(viewModel.to)))).build())
                    .build();
            datePicker.show(getParentFragmentManager(), "datePicker");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                viewModel.reservationFrom = selection;
                binding.from.setText(datePicker.getHeaderText());
                regetReservation();
            });
        });
        binding.from.setText(dateFormat.format(viewModel.reservationFrom));
        binding.to.setText(dateFormat.format(viewModel.reservationTo));
        binding.to.setOnClickListener(_ -> {
            MaterialDatePicker<Long> datePicker = picker
                    .setSelection(viewModel.reservationTo)
                    .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointForward.from(viewModel.reservationFrom)))).build())
                    .build();
            datePicker.show(getParentFragmentManager(), "datePicker");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                viewModel.reservationTo = selection;
                binding.to.setText(datePicker.getHeaderText());
                regetReservation();
            });
        });
        getReservation();
        return binding.getRoot();
    }

    private void regetReservation() {
        reset();
        getReservation();
    }

    void reset() {
        concatAdapter.getAdapters().forEach(adapter -> concatAdapter.removeAdapter(adapter));
    }

    void getReservation() {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + String.format("api/BookingRequestVenue?all=false&startDate=%s&endDate=%s&waitingList=true", dateFormat.format(viewModel.reservationFrom), dateFormat.format(viewModel.reservationTo)), 0);
    }
    void deleteReservation(String bookingId) {
        http.deleteRequest(viewModel.authorizationManager.getBaseUrl() + String.format("api/BookingRequestVenue/%s", bookingId), 1);
    }

}
