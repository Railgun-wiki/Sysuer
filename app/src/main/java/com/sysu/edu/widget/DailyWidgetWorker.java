package com.sysu.edu.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sysu.edu.R;

import java.util.List;

public class DailyWidgetWorker extends Worker {

    public DailyWidgetWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        List.of(TodayClassWidget.class, TomorrowClassWidget.class).forEach(c -> appWidgetManager.updateAppWidget(new ComponentName(getApplicationContext(), c), new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_today_class)));
        return Result.success();
    }
}
