package com.sysu.edu;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ClassWidgetWorker extends Worker {

    public ClassWidgetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        appWidgetManager.updateAppWidget(new ComponentName(getApplicationContext(), ClassWidget.class), new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_init));
        return Result.success();
    }
}
