package com.sysu.edu.widget;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class RecentClassWidgetWorker extends Worker {


    public RecentClassWidgetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        getApplicationContext().startService(new Intent(getApplicationContext(), RecentClassWidget.class));
        return Result.success();
    }
}
