package com.sysu.edu.academic;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemEvaluationBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;

import java.util.ArrayList;
import java.util.Objects;

public class EvaluationCategoryFragment extends Fragment {
    HttpManager http;
    StaggeredGridLayoutManager staggeredGridLayoutManager;
    Params params;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        params = new Params(requireActivity());
        params.setCallback(this, this::getEvaluation);
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(params.getColumn(), 1);
        binding.getRoot().setLayoutManager(staggeredGridLayoutManager);
        CategoryAdapter categoryAdapter = new CategoryAdapter(requireContext());
        categoryAdapter.setKeys(new String[]{"rwmc", "rwkssj", "rwjssj", "pjsl", "ypsl"});
        categoryAdapter.setValues(new String[]{"%s", "起始时间：%s", "结束时间：%s", "总评数：%s", "已评数：%s"});
        categoryAdapter.setParams(new String[]{"rwid", "firstwjid", "pjrdm"});
        categoryAdapter.setNavigation(R.id.from_category_to_course);
        binding.getRoot().setAdapter(categoryAdapter);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    JSONObject data = JSON.parseObject((String) msg.obj);
                    if (data != null && Objects.equals(data.get("code"), "200")) {
                        data.getJSONObject("result").getJSONArray("list").forEach(e -> categoryAdapter.add((JSONObject) e));
                    } else {
                        params.gotoLogin(getView(), "https://pjxt.sysu.edu.cn");
                    }
                } else if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                    params.gotoLogin(getView(), "https://pjxt.sysu.edu.cn");
                }
            }
        });
        http.setParams(params);
        getEvaluation();

        return binding.getRoot();
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        staggeredGridLayoutManager.setSpanCount(params.getColumn());
    }

    public void getEvaluation() {
        http.getRequest("https://pjxt.sysu.edu.cn/personnelEvaluation/listObtainPersonnelEvaluationTasks?pageNum=1&pageSize=10", 1);
    }

    public static class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final Context context;
        final ArrayList<JSONObject> data = new ArrayList<>();
        String[] keys;
        String[] values;
        String[] params;
        int nav;

        public CategoryAdapter(Context context) {
            super();
            this.context = context;
        }

        public void add(JSONObject e) {
            data.add(e);
            notifyItemInserted(data.size() - 1);
        }

        public void clear() {
            int tmp = getItemCount();
            data.clear();
            notifyItemMoved(0, tmp);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(context), parent, false).getRoot()) {
            };
        }

        public void setKeys(String[] keys) {
            this.keys = keys;
        }

        public void setValues(String[] values) {
            this.values = values;
        }

        public void setParams(String[] params) {
            this.params = params;
        }

        public void setNavigation(int nav) {
            this.nav = nav;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemEvaluationBinding binding = ItemEvaluationBinding.bind(holder.itemView);
            Bundle args = new Bundle();
            for (String param : params) args.putString(param, data.get(position).getString(param));
            binding.open.setOnClickListener(_ -> ((NavHostFragment) Objects.requireNonNull(((AppCompatActivity) context).getSupportFragmentManager().findFragmentById(R.id.fragment))).getNavController().navigate(nav, args));
            holder.itemView.setOnClickListener(_ -> {
            });
            Drawable drawable = AppCompatResources.getDrawable(context, Integer.parseInt(data.get(position).getString("pjsl")) <= Integer.parseInt(data.get(position).getString("ypsl")) ? R.drawable.submit : R.drawable.window);
            if (drawable != null) drawable.setBounds(0, 0, 72, 72);
            binding.title.setCompoundDrawables(drawable, null, null, null);
            binding.title.setCompoundDrawablePadding(36);
            binding.title.setText(String.format(values[0], data.get(position).getString(keys[0]) == null ? "" : data.get(position).getString(keys[0])));
            StringBuilder val = new StringBuilder();
            for (int i = 1; i < keys.length; i++)
                val.append(String.format(values[i], data.get(position).getString(keys[i]) == null ? "" : data.get(position).getString(keys[i]))).append("\n");
            binding.startTime.setText(val.toString().trim());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}