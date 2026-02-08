package com.sysu.edu.view;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public interface AdapterListener {
    void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position);

    void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding);

}
