package com.sysu.edu.api;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RequiresApi(api = Build.VERSION_CODES.O)
public class NewCalendarManager {


    private final LocalDate date = LocalDate.now();

    public NewCalendarManager() {
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDateString(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }
}
