package com.sysu.edu.todo.info;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.sysu.edu.databinding.ItemTodoBinding;
import com.sysu.edu.template.RecyclerAdapter;
import com.sysu.edu.todo.InitTodo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodoAdapter extends RecyclerAdapter<TodoInfo> {
    private final InitTodo initTodo;

    public TodoAdapter(InitTodo initTodo) {
        super();
        this.initTodo = initTodo;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemTodoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemTodoBinding binding = ItemTodoBinding.bind(holder.itemView);
        Context context = binding.getRoot().getContext();
        TodoInfo item = get(position);
        binding.title.setText(item.getTitle().getValue());
        binding.description.setText(item.getDescription().getValue());
        //binding.dueDate.setText(item.getDueDate());
        binding.getRoot().setOnClickListener(_ -> {
            initTodo.initDialog(item);
            initTodo.showDialog();
        });
        //boolean isCheck = item.getStatus().getValue() != null && item.getStatus().getValue() == 1;

        //System.out.println(isCheck ? 0.5f : 1.0f);
        binding.check.setOnCheckedChangeListener((_, isChecked) -> {
            item.setStatus(isChecked ? TodoInfo.DONE : TodoInfo.TODO);
            //notifyItemChanged(position);
        });
        item.getStatus().observe((FragmentActivity) context, status -> {
            boolean isCheck = status != null && status.equals(TodoInfo.DONE);
            binding.getRoot().setAlpha(isCheck ? 0.5f : 1.0f);
            binding.check.setChecked(isCheck);
            binding.title.setPaintFlags(isCheck ? binding.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.description.setPaintFlags(isCheck ? binding.description.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.description.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.menu.setEnabled(!isCheck);
            //binding.check.setChecked(isChecked);
//            binding.title.setPaintFlags(isChecked ? binding.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
//            binding.description.setPaintFlags(isChecked ? binding.description.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.description.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
//            binding.menu.setEnabled(!isChecked);
            item.setDoneDate(isCheck ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) : null);
            initTodo.updateTodo(item);
        });
        //binding.dueDate.setText(item.get("due_date"));
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }
}
