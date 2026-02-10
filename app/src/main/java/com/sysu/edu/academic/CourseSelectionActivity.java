package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityCourseSelectionBinding;
import com.sysu.edu.view.Pager2Adapter;

import java.util.List;
import java.util.stream.IntStream;

public class CourseSelectionActivity extends AppCompatActivity {

    ActivityCourseSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseSelectionBinding.inflate(getLayoutInflater());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        setContentView(binding.getRoot());
//        NavController navController = ((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.course_selection_fragment))).getNavController();
//        binding.tab.addTab(binding.tab.newTab().setText(R.string.course_selection));FF
//        binding.tab.addTab(binding.tab.newTab().setText(R.string.preview));
//        binding.tab.addTab(binding.tab.newTab().setText(R.string.course_selected));
        Pager2Adapter adp = new Pager2Adapter(this);
        binding.pager2.setAdapter(adp);
        new TabLayoutMediator(binding.tab, binding.pager2, (tab, position) -> tab.setText(List.of(R.string.course_selection, R.string.preview, R.string.course_selected).get(position))).attach();
        IntStream.range(0, 3).forEach(i -> adp.add(CourseSelectionContainerFragment.newInstance(i)));
//        adp.add(new CourseSelectionFragment());
//        adp.add(new CourseSelectionPreviewFragment());
//        adp.add(new CourseSelectionSelectedFragment());
       /* binding.tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                navController.navigate(new int[]{R.id.selection_navigation, R.id.preview_navigation, R.id.course_selection_selected_fragment}[tab.getPosition()], null,
                        new NavOptions.Builder().setRestoreState(true)*//*.setPopUpTo(R.id.selection_navigation, true, true)*//*.setLaunchSingleTop(true).build());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });*/
//        NavigationUI.setupWithNavController(binding.toolbar, navController, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
//            supportFinishAfterTransition();
//            return true;
//        }).build());
//        navController.addOnDestinationChangedListener((_, destination, _) -> binding.tab.selectTab(binding.tab.getTabAt(destination.getId() == R.id.preview_fragment ? 1 : destination.getId() == R.id.course_selection_selected_fragment ? 2 : 0)));
        //binding.toolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}