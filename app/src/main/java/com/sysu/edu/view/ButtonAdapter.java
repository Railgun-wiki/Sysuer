package com.sysu.edu.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sysu.edu.databinding.ItemButtonBinding;
import com.sysu.edu.databinding.ItemButtonGroupBinding;
import com.sysu.edu.template.RecyclerAdapter;

public class ButtonAdapter extends RecyclerAdapter<String> {
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout itemView = ItemButtonGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
        for (String button : data) {
            ItemButtonBinding binding = ItemButtonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            binding.getRoot().getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            binding.getRoot().setText(button);
            binding.getRoot().setStrokeWidth(3);
            binding.getRoot().setStrokeColorResource(com.google.android.material.R.color.m3expressive_button_outline_color_selector);
            if (listener != null) listener.onCreate(this, binding);
            itemView.addView(binding.getRoot());
        }
        return new RecyclerView.ViewHolder(itemView) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
