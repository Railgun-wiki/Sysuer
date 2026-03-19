package com.sysu.edu.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sysu.edu.databinding.ItemButtonGroupBinding;
import com.sysu.edu.databinding.ItemButtonOutlineBinding;
import com.sysu.edu.template.RecyclerAdapter;

public class ButtonAdapter extends RecyclerAdapter<String> {
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout itemView = ItemButtonGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
        for (String button : data) {
            ItemButtonOutlineBinding binding = ItemButtonOutlineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            binding.getRoot().setText(button);
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
