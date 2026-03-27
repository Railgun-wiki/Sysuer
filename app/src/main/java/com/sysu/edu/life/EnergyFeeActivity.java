package com.sysu.edu.life;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityWaterEletricityFeeBinding;

public class EnergyFeeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityWaterEletricityFeeBinding binding = ActivityWaterEletricityFeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        if (fragment != null) {
            NavController nav = fragment.getNavController();
//            NavHostController navHostController = new NavHostController(this).getNavController();
            NavigationUI.setupWithNavController(binding.toolbar, nav, new AppBarConfiguration.Builder().setFallbackOnNavigateUpListener(() -> {
                supportFinishAfterTransition();
                return false;
            }).build());
            NavigationUI.setupWithNavController(binding.bottomNavigation, nav);
        }
//        NavigationContainer exampleContainer = new ActivityNavigationContainer(getApplicationContext());
    }

}


