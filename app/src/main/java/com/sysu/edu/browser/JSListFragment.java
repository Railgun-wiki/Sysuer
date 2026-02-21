package com.sysu.edu.browser;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.transition.MaterialContainerTransform;
import com.sysu.edu.R;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.view.AdapterListener;

import java.util.Map;

public class JSListFragment extends Fragment {

    BrowserHelper db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(getLayoutInflater());
        db = new BrowserHelper(requireContext());
        BrowserActivity.JSAdapter jsAdapter = new BrowserActivity.JSAdapter();

        jsAdapter.setListener(new AdapterListener() {
            @Override
            public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setOnClickListener(v -> {
                    v.setTransitionName("script");
                    Bundle bundle = new Bundle();
                    bundle.putString("script", jsAdapter.get(position).getString("script"));
                    bundle.putString("title", jsAdapter.get(position).getString("title"));
                    Navigation.findNavController(binding.getRoot()).navigate(R.id.list_to_editor, bundle, null, new FragmentNavigator.Extras(Map.of(v, "script")));
                });
            }

            @Override
            public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding1) {
            }
        });
        binding.getRoot().setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.getRoot().setAdapter(jsAdapter);

        Cursor cursor = db.getReadableDatabase().query("js", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                jsAdapter.add(new JSONObject().fluentPut("title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                        .fluentPut("description", cursor.getString(cursor.getColumnIndexOrThrow("description")))
                        .fluentPut("matches", JSONArray.parse(cursor.getString(cursor.getColumnIndexOrThrow("matches"))))
                        .fluentPut("script", cursor.getString(cursor.getColumnIndexOrThrow("script"))));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialContainerTransform transition = new MaterialContainerTransform();
        transition.setScrimColor(Color.TRANSPARENT);
        transition.setAllContainerColors(requireContext().getColor(com.google.android.material.R.color.design_default_color_surface));
        setSharedElementEnterTransition(transition);
        setSharedElementReturnTransition(transition);
    }
}