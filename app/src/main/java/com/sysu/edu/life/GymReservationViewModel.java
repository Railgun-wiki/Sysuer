package com.sysu.edu.life;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sysu.edu.api.AuthorizationManager;

public class GymReservationViewModel extends ViewModel {
    final String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";
    //    final MutableLiveData<String> authorization = new MutableLiveData<>("");
//    String cookie = "";
    final AuthorizationManager authorizationManager = new AuthorizationManager("https://gym.sysu.edu.cn/", "https://gym-443.webvpn.sysu.edu.cn/");
//    final MutableLiveData<Boolean> loginRequired = new MutableLiveData<>();

    long from = System.currentTimeMillis();
    long to = System.currentTimeMillis();

    long reservationFrom = System.currentTimeMillis();
    long reservationTo = System.currentTimeMillis();

    final MutableLiveData<Integer> position = new MutableLiveData<>();
}
