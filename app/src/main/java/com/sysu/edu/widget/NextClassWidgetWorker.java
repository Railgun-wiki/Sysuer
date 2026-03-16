package com.sysu.edu.widget;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NextClassWidgetWorker extends Worker {

    public NextClassWidgetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        getApplicationContext().startService(new Intent(getApplicationContext(), NextClassWidget.class));
        return Result.success();
    }
}
