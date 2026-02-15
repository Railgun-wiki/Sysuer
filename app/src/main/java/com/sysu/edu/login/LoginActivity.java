package com.sysu.edu.login;

import static com.sysu.edu.login.LoginManager.initLoginModel;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityLoginBinding;
import com.sysu.edu.view.Pager2Adapter;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.pager2.setAdapter(new Pager2Adapter(this).add(new LoginWebFragment()).add(new LoginFragment()));
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        new TabLayoutMediator(binding.options, binding.pager2, (tab, i) -> tab.setText(new int[]{R.string.web_login, R.string.password_login}[i])).attach();
        initLoginModel(this, new ViewModelProvider(this).get(LoginViewModel.class), getIntent().getStringExtra("url") == null ? "https://jwxt.sysu.edu.cn/jwxt/yd/index/#/Home" : getIntent().getStringExtra("url"), () -> {
            setResult(RESULT_OK);
            supportFinishAfterTransition();
        });
    }
}