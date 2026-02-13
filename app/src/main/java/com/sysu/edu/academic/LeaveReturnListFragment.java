package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ItemCardBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class LeaveReturnListFragment extends StaggeredFragment {

    View view;
    HttpManager http;
    AuthorizationManager authorizationManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = super.onCreateView(inflater, container, savedInstanceState);
        LeaveReturnRegistrationViewModel viewModel = new ViewModelProvider(requireActivity()).get(LeaveReturnRegistrationViewModel.class);
        viewModel.year.observe(getViewLifecycleOwner(), this::getList);
        authorizationManager = new AuthorizationManager("https://xgxt.sysu.edu.cn/", "https://xgxt-443.webvpn.sysu.edu.cn/");
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else if (msg.what == 0) {
                    int code = msg.getData().getInt("code");
                    boolean isJSON = msg.getData().getBoolean("isJSON");
                    if (isJSON) {
                        if (code == 200) {
                            JSONObject json = JSONObject.parse((String) msg.obj);
                            if (json != null && json.getInteger("code") == 200) {
                                clear();
                                json.getJSONArray("data").forEach(e -> add(((JSONObject) e).getString("gzmc"), ((JSONObject) e).getInteger("gzztm") == 1 ? R.drawable.uncheck : R.drawable.check, List.of(getResources().getStringArray(R.array.registration_keys)),
                                        extractValue((JSONObject) e, new String[]{"blxn", "lxdjsj", "gzsm", "jjrmc", "jjrrq", "gzzt", "zt"})));
                                setListener(new AdapterListener() {
                                    @Override
                                    public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                                        boolean isRegistering = json.getJSONArray("data").getJSONObject(position).getInteger("gzztm") == 1;
                                        String status = json.getJSONArray("data").getJSONObject(position).getString("zt");
                                        MaterialButton button = holder.itemView.findViewById(R.id.button);
                                        button.setText(isRegistering ? status.equals("registering") ? R.string.start_registration : R.string.modify_registration : R.string.view_detail);
                                        button.setOnClickListener(_ -> {
                                            if (isRegistering) {
                                                Bundle arg = new Bundle();
                                                arg.putString("Id", json.getJSONArray("data").getJSONObject(position).getString("cjlfxgzId"));
                                                requireActivity().getSupportFragmentManager()
                                                        .beginTransaction()
                                                        .replace(R.id.leave_return_list_fragment, LeaveReturnRegistrationFragment.class, arg)
                                                        .addToBackStack(null)
                                                        .commit();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
                                        MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle);
                                        button.setId(R.id.button);
                                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        lp.gravity = Gravity.END;
                                        lp.setMargins(0, 0, params.dpToPx(16), params.dpToPx(16));
                                        button.setLayoutParams(lp);
                                        ((ItemCardBinding) binding).getRoot().addView(button);
                                    }
                                });
                            } else if (json != null) {
                                params.toast(json.getString("msg"));
                            }
                        } else {
                            params.toast(R.string.login_warning);
                            params.gotoLogin(getView(), TargetUrl.XGXT);
                        }
                    } else {
                        params.toast(R.string.educational_wifi_warning);
                        authorizationManager.setAccessible(false);
                        getList(viewModel.year.getValue());
                    }
                }
            }
        });
        http.setParams(params);
        return view;
    }

    void getList(String year) {
        http.getRequest(authorizationManager.getBaseUrl() + "jjrlfx/api/sm-jjrlfx/student/work-list?blxn=" + year, 0);
    }

}