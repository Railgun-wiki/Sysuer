package com.sysu.edu;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ClassUpdate extends Worker {

    public ClassUpdate(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ClassIsland.sendCourseNotification(getApplicationContext(), "测试", "测试", "测试");
        System.out.println("ClassUpdate");
        return Result.success();
    }
}
