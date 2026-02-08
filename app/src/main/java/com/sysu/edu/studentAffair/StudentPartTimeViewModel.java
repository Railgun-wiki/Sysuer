package com.sysu.edu.studentAffair;

import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sysu.edu.view.EditTextDialog;

public class StudentPartTimeViewModel extends ViewModel {
    public PopupMenu yearPop;
    public PopupMenu campusPop;
    public PopupMenu typePop;

    public MutableLiveData<String> year = new MutableLiveData<>("2026");
    public MutableLiveData<String> jobType = new MutableLiveData<>("");
    public MutableLiveData<String> campus = new MutableLiveData<>("");
    public EditTextDialog jobNameDialog;
    public EditTextDialog unitDialog;

    public MutableLiveData<String> yearName = new MutableLiveData<>("2026");
    public MutableLiveData<String> jobTypeName = new MutableLiveData<>("");
    public MutableLiveData<String> campusName = new MutableLiveData<>("");
    public MutableLiveData<String> jobName = new MutableLiveData<>("");
    public MutableLiveData<String> unitName = new MutableLiveData<>("");


}
