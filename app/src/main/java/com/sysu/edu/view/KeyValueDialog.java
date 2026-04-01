package com.sysu.edu.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sysu.edu.databinding.DialogKeyValueBinding;
import com.sysu.edu.life.GymAccountFragment;

public class KeyValueDialog {
    private final BottomSheetDialog dialog;
    private final DialogKeyValueBinding binding;
    private GymAccountFragment.PreferenceAdapter adapter;

    public KeyValueDialog(Context context) {
        dialog = new BottomSheetDialog(context);
        binding = DialogKeyValueBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());
        init(context);
    }

    private void init(Context context) {
        binding.recyclerView.getRoot().setLayoutManager(new LinearLayoutManager(context));
        adapter = new GymAccountFragment.PreferenceAdapter();
        binding.recyclerView.getRoot().setAdapter(adapter);
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }

    public GymAccountFragment.PreferenceAdapter getAdapter() {
        return adapter;
    }

    public void setPositiveButton(String text, View.OnClickListener onClick) {
        binding.positive.setVisibility(View.VISIBLE);
        binding.positive.setText(text);
        binding.positive.setOnClickListener(onClick);
    }

    public void setNegativeButton(String text, View.OnClickListener onClick) {
        binding.negative.setVisibility(View.VISIBLE);
        binding.negative.setText(text);
        binding.negative.setOnClickListener(onClick);
    }
    public void add(String title, String content, Integer icon) {
        adapter.add(title, content, icon);
    }
    public void clear() {
        adapter.clear();
    }
}
