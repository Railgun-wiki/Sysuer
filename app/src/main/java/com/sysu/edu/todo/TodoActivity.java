package com.sysu.edu.todo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sysu.edu.databinding.ActivityTodoBinding;

public class TodoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTodoBinding binding = ActivityTodoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.add.setOnClickListener(_ -> ((TodoFragment) binding.fragment.getFragment()).getInitTodo().showTodoAddDialog());
    }
}
