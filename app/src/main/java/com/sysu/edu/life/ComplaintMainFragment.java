package com.sysu.edu.life;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.sysu.edu.R;

public class ComplaintMainFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_complaint_main, container, false);
    }
}