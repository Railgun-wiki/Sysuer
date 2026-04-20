package com.sysu.edu.life;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import com.sysu.edu.databinding.FragmentComplaintMainBinding;

public class ComplaintMainFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentComplaintMainBinding binding = FragmentComplaintMainBinding.inflate(inflater, container, false);
        binding.captchaImage.setOnClickListener(_ -> loadCaptcha(binding.captchaImage));
        loadCaptcha(binding.captchaImage);
        return binding.getRoot();
    }

    void loadCaptcha(ShapeableImageView imageView) {
        Glide.with(requireContext()).load(Uri.parse("https://xinfang.sysu.edu.cn/servlet/checkcode")).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).override(300, 120).into(imageView);
    }

}