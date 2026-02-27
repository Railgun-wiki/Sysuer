package com.sysu.edu.template;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sysu.edu.view.AdapterListener;

import java.util.ArrayList;
import java.util.Collections;

public abstract class RecyclerAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected final ArrayList<T> data = new ArrayList<>();

    protected AdapterListener listener;

    @Override
    public int getItemCount() {
        return data.size();
    }

    /*@NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewBinding binding = ViewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        if (listener != null) listener.onCreate(this, binding);
        return new RecyclerView.ViewHolder(binding.getRoot()) {
        };
    }*/

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (listener != null) listener.onBind(this, holder, position);
    }

    public void add(T item) {
        data.add(item);
        notifyItemInserted(getItemCount() - 1);
    }

    public void remove(int position) {
        data.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, position - 1);
        notifyItemRangeChanged(position, getItemCount() - position);
    }

    public void clear() {
        int temp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0, temp);
    }

    public T get(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return data.get(position);
    }

    public void setListener(AdapterListener listener) {
        this.listener = listener;
    }

    public void set(ArrayList<T> d) {
        clear();
        data.addAll(d);
        notifyItemRangeInserted(0, getItemCount());
    }

    public void swap(int position1, int position2) {
        Collections.swap(data, position1, position2);
        notifyItemMoved(position1, position2);
    }
}
