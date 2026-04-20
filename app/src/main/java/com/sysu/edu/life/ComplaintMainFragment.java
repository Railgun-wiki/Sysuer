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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ComplaintMainFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentComplaintMainBinding binding = FragmentComplaintMainBinding.inflate(inflater, container, false);
        binding.captchaImage.setOnClickListener(_ -> loadCaptcha(binding.captchaImage));
        loadCaptcha(binding.captchaImage);
        return binding.getRoot();
    }

    private void loadCaptcha(ShapeableImageView captcha) {

        try {
            captcha.setImageBitmap(CompletableFuture.supplyAsync(() -> {
                try {
                return Glide.with(requireContext()).asBitmap().load(Uri.parse("https://xinfang.sysu.edu.cn/servlet/checkcode")).diskCacheStrategy(DiskCacheStrategy.NONE).override(300, 120).submit().get();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).get());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}