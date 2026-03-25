package com.sysu.edu.life;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.FragmentWaterFeeBinding;

public class EnergyWaterFeeFragment extends Fragment {


    HttpManager http;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Params params = new Params(this);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1)
                    params.toast(R.string.no_wifi_warning);
                else if (msg.getData().getBoolean("isJSON")) {
//                    switch (msg.what) {
//                        case 0 -> params.toast(R.string.dorm_info_success);
//                        case 1 -> params.toast(R.string.dorm_info_failed);
//                    }
                }
                super.handleMessage(msg);
            }
        });
        http.setAuthorizationRequired(true);
        http.setAuthorizationJar(new AuthorizationJar(requireContext()));
        FragmentWaterFeeBinding binding = FragmentWaterFeeBinding.inflate(inflater, container, false);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));


        return binding.getRoot();
    }


}
