package com.sysu.edu.home;

import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class HomeViewModel extends ViewModel {

    public MutableLiveData<Boolean> updateDashboardShortcut = new MutableLiveData<>();

    public Map<Integer, View.OnClickListener> actionMap = new HashMap<>();

}
