package com.sysu.edu.studentAffair;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityStudentPartTimeBinding;

import java.util.Objects;

public class StudentPartTime extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStudentPartTimeBinding binding = ActivityStudentPartTimeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.getRoot(), binding.toolbar, R.string.open, R.string.close);
        toggle.syncState();
        binding.getRoot().addDrawerListener(toggle);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        NavController navController = Objects.requireNonNull(navHostFragment).getNavController();
//        NavigationUI.setupWithNavController(binding.toolbar, navController, binding.getRoot());
        NavigationUI.setupWithNavController(binding.navView, navController);

    }
}
