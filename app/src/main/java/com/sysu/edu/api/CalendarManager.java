package com.sysu.edu.api;

import android.icu.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarManager {

    final Calendar calendar = Calendar.getInstance();

    public int getYear() {
        return calendar.get(Calendar.YEAR);
    }

    public String toDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }
    public String getDateTime(Calendar calendar) {
        return getDateTime(calendar.getTime());
    }

    public String getDateTime(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).format(date);
    }

    public Calendar getFirstOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, this.calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public Calendar getEndOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, this.calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar;
    }
}