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
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentEnergyOrderBinding;
import com.sysu.edu.todo.info.TitleAdapter;

import java.util.List;

import dev.enro.annotations.NavigationDestination;

@NavigationDestination(key = HomeKey.class)
public class EnergyOrderFragment extends Fragment{

    HttpManager http;
    String roomCode;
    String username;
    FragmentEnergyOrderBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        System.out.println("onCreateView " + savedInstanceState);
        System.out.println(binding);
        System.out.println(savedInstanceState);
        System.out.println(roomCode);
        if (binding == null) {
            Params params = new Params(this);
            ConcatAdapter adapter = new ConcatAdapter();
            ContextUtil contextUtil = new ContextUtil(requireContext());
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    System.out.println(msg.obj);
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                    } else if (msg.getData().getBoolean("isJSON")) {
                        JSONObject response = JSONObject.parse((String) msg.obj);
                        if (response.getInteger("code") == 200) {
                            switch (msg.what) {
                                case 0 -> {
                                    JSONObject userInfo = response.getJSONObject("data");
                                    adapter.addAdapter(new TitleAdapter(getString(R.string.account)));
                                    GymAccountFragment.PreferenceAdapter preferenceAdapter = new GymAccountFragment.PreferenceAdapter();
                                    preferenceAdapter.set(List.of(R.string.name, R.string.student_id),
                                            extractValue(userInfo, new String[]{"name", "username"}), List.of(R.drawable.account, R.drawable.id), requireContext());
                                    adapter.addAdapter(preferenceAdapter);
                                    username = userInfo.getString("username");
                                    getRoom(username);
                                }
                                case 1 -> response.getJSONArray("data").forEach(e -> {
                                    JSONObject roomInfo = (JSONObject) e;
                                    adapter.addAdapter(new TitleAdapter(getString(R.string.dorm)));
                                    GymAccountFragment.PreferenceAdapter preferenceAdapter = new GymAccountFragment.PreferenceAdapter();
                                    preferenceAdapter.set(List.of(R.string.location, R.string.room_name),
                                            extractValue(roomInfo, new String[]{"areaInfo", "roomName"}), List.of(R.drawable.location, R.drawable.home), requireContext());
                                    adapter.addAdapter(preferenceAdapter);
                                    roomCode = roomInfo.getString("roomId");
                                });
                            }
                        } else contextUtil.login(TargetUrl.ZHNY, () -> getUserInfo());
                    }
                    super.handleMessage(msg);
                }
            });
            http.setAuthorizationRequired(true);
            http.setAuthorizationJar(new AuthorizationJar(requireContext()));
            binding = FragmentEnergyOrderBinding.inflate(inflater, container, false);
            binding.recyclerViewScroll.getRoot().setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerViewScroll.getRoot().setAdapter(adapter);
            getUserInfo();
        }
        return binding.getRoot();
    }

    void getUserInfo() {
        http.getRequest("https://zhny.sysu.edu.cn/kbp/auth/userInfo", 0);
    }

    void getRoom(String username) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/admin/sys/personRoom/list", "{\"username\":\"" + username + "\"}", 1);
    }


}
//
//class EnergyOrder implements NavigationKey.SupportsPush {
//
//    String date;
//
//    public EnergyOrder(String date) {
//        this.date = date;
//    }
//
//    static final Creator<String> CREATOR = new Creator<>() {
//
//        @Override
//        public String createFromParcel(Parcel source) {
//            return source.readString();
//        }
//
//        @Override
//        public String[] newArray(int size) {
//            return new String[size];
//        }
//    };
//    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    @Override
//    public void writeToParcel(@NonNull Parcel dest, int flags) {
//    }
//}
