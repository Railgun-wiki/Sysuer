package com.sysu.edu.widget;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class DailyWidgetWorker extends Worker {

    public DailyWidgetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        List.of(TodayClassWidget.class, TomorrowClassWidget.class).forEach(c -> getApplicationContext().startService(new Intent(getApplicationContext(), c)));
        return Result.success();
    }
}
