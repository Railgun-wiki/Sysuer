package com.sysu.edu.studentAffair;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityStudentPartTimeBinding;
import com.sysu.edu.view.EditTextDialog;

import java.util.Objects;

public class StudentPartTimeActivity extends AppCompatActivity {


    StudentPartTimeViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStudentPartTimeBinding binding = ActivityStudentPartTimeBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(StudentPartTimeViewModel.class);
        viewModel.campusPop = new PopupMenu(this, binding.campus, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        viewModel.typePop = new PopupMenu(this, binding.jobType, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        viewModel.yearPop = new PopupMenu(this, binding.year, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        viewModel.jobNameDialog = new EditTextDialog(this);
        viewModel.jobNameDialog.setTitle(R.string.job_name);
        viewModel.jobNameDialog.setHint(R.string.job_name);

        viewModel.unitDialog = new EditTextDialog(this);
        viewModel.unitDialog.setTitle(R.string.unit);
        viewModel.unitDialog.setHint(R.string.unit);
        setContentView(binding.getRoot());
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.getRoot(), binding.toolbar, R.string.open, R.string.close);
        toggle.syncState();
        binding.getRoot().addDrawerListener(toggle);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        NavController navController = Objects.requireNonNull(navHostFragment).getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);
        /*binding.toolbar.getMenu().add(R.string.export).setIcon(R.drawable.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener(
                item -> {
                    return true;
                }
        );*/
        binding.navView.setNavigationItemSelectedListener(item -> {
            NavigationUI.onNavDestinationSelected(item, navController);
            binding.filter.animate().alpha(item.getItemId() == R.id.recruitment_info ? 1 : 0)
                    .withStartAction(() -> binding.filter.setVisibility(View.VISIBLE))
                    .withEndAction(() -> binding.filter.setVisibility(item.getItemId() == R.id.recruitment_info ? View.VISIBLE : View.GONE));
            binding.getRoot().closeDrawer(GravityCompat.START, true);
            return true;
        });

        binding.year.setOnClickListener(_ -> viewModel.yearPop.show());
        binding.campus.setOnClickListener(_ -> viewModel.campusPop.show());
        binding.jobType.setOnClickListener(_ -> viewModel.typePop.show());
        binding.jobName.setOnClickListener(_ -> viewModel.jobNameDialog.show());
        binding.unit.setOnClickListener(_ -> viewModel.unitDialog.show());
        viewModel.yearName.observe(this, year -> {
            if (year.isEmpty())
                binding.year.setText(R.string.year);
            else
                binding.year.setText(year);
        });
        viewModel.campusName.observe(this, campus -> {
            if (campus.isEmpty())
                binding.campus.setText(R.string.campus);
            else
                binding.campus.setText(campus);
        });
        viewModel.jobTypeName.observe(this, jobType -> {
            if (jobType.isEmpty())
                binding.jobType.setText(R.string.job_type);
            else
                binding.jobType.setText(jobType);
        });
        viewModel.jobName.observe(this, jobName -> {
            if (jobName.isEmpty())
                binding.jobName.setText(R.string.job_name);
            else
                binding.jobName.setText(jobName);
        });
        viewModel.unitName.observe(this, unit -> {
            if (unit.isEmpty())
                binding.unit.setText(R.string.unit);
            else
                binding.unit.setText(unit);
        });
    }
}
