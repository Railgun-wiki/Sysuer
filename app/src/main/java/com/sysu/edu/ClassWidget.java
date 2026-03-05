package com.sysu.edu;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.api.HttpManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ClassWidget extends AppWidgetProvider {

    final ArrayList<JSONObject> todayCourse = new ArrayList<>();
    final ArrayList<JSONObject> tomorrowCourse = new ArrayList<>();
    HttpManager http;

    private static void update(AppWidgetManager appWidgetManager, int[] appWidgetIds, RemoteViews remoteViews, Context context) {
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_init);
//                new WorkManager(context).enqueue(new OneTimeWorkRequest.Builder(ClassUpdate.class).build());
                if (msg.what == -1) {
                    remoteViews.setTextViewText(R.id.week, context.getString(R.string.login_warning));
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response.get("code").equals(200)) {
                        switch (msg.what) {
                            case 1:
                                ArrayList<JSONObject> beforeArray = new ArrayList<>();
                                ArrayList<JSONObject> afterArray = new ArrayList<>();
                                response.getJSONArray("data").forEach(e -> {
                                    JSONObject jsonObject = (JSONObject) e;
                                    String status = getTimePosition(jsonObject.getString("teachingDate") + " " + jsonObject.getString("startTime"), jsonObject.getString("teachingDate") + " " + jsonObject.getString("endTime"));
                                    jsonObject.put("status", status);
                                    jsonObject.put("time", jsonObject.get("startTime") + "~" + jsonObject.get("endTime"));
                                    jsonObject.put("course", "第" + jsonObject.get("startClassTimes") + "~" + jsonObject.get("endClassTimes") + "节课");
                                    String flag = (String) jsonObject.get("useflag");
                                    if ("TD".equals(flag))
                                        (Objects.equals(status, "before") ? beforeArray : afterArray).add(jsonObject);
                                    ("TD".equals(flag) ? todayCourse : tomorrowCourse).add(jsonObject);
//                                    addCourse(flag.equals("TD") ? todayCourse : tomorrowCourse, (String) ((JSONObject) e).get("courseName"), (String) ((JSONObject) e).get("teachingPlace"), ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime")
//                                            , "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课", (String) ((JSONObject) e).get("teacherName"), flag);
                                });
//                                binding.courseList.scrollToPosition(beforeArray.size());


                                try {
                                    JSONObject array = afterArray.isEmpty() ? tomorrowCourse.get(0) : todayCourse.get(beforeArray.size());
                                    Date target = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.getDefault()).parse(String.format("%s %s",
                                            array.getString("teachingDate"), array.getString("endTime")));
                                    if (target != null) {
//                                        System.out.println(target.getTime() - System.currentTimeMillis());
                                        WorkManager.getInstance(context.getApplicationContext())
                                                .enqueueUniqueWork("next_class_widget_update",
                                                        ExistingWorkPolicy.KEEP, new OneTimeWorkRequest.Builder(ClassUpdate.class).setInitialDelay(target.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS).build());

                                    }
                                } catch (ParseException _) {
                                }

                                remoteViews.setTextViewText(R.id.content,
                                        afterArray.isEmpty() ? String.format("%s:%s\n%s:%s %s",
                                                context.getString(R.string.location), tomorrowCourse.isEmpty() ? context.getString(R.string.none) : tomorrowCourse.get(0).getString("teachingPlace"),
                                                context.getString(R.string.time),
                                                tomorrowCourse.get(0).getString("teachingDate"), tomorrowCourse.isEmpty() ? context.getString(R.string.none) : tomorrowCourse.get(0).getString("time")) :
                                                String.format("%s:%s\n%s:%s %s",
                                                        context.getString(R.string.location), todayCourse.get(beforeArray.size()).getString("teachingPlace"),
                                                        context.getString(R.string.time),
                                                        todayCourse.get(beforeArray.size()).getString("teachingDate"), todayCourse.get(beforeArray.size()).getString("time"))
                                        /*Markwon.builder(context).usePlugin(new AbstractMarkwonPlugin() {
                                    @Override
                                    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                                        super.configureSpansFactory(builder);
                                        builder.appendFactory(Heading.class, (_, configuration) -> {
                                            if (CoreProps.HEADING_LEVEL.require(configuration) <= 3)
                                                return new ForegroundColorSpan(Color.parseColor("#6750a4"));
                                            return null;
                                        });
                                    }

                                    @Override
                                    public void configureVisitor(@NonNull MarkwonVisitor.Builder builder) {
                                        super.configureVisitor(builder);
                                        builder.blockHandler(new MarkwonVisitor.BlockHandler() {
                                            @Override
                                            public void blockStart(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                                            }

                                            @Override
                                            public void blockEnd(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                                                if (visitor.hasNext(node))
                                                    visitor.ensureNewLine();
                                            }
                                        });
                                    }
                                }).build().toMarkdown()*/);
                                remoteViews.setTextViewText(R.id.title, afterArray.isEmpty() ? tomorrowCourse.isEmpty() ? context.getString(R.string.none) : tomorrowCourse.get(0).getString("courseName") :
                                        todayCourse.get(beforeArray.size()).getString("courseName"));
                                break;
                            /*case 2:
                                JSONArray dataArray = response.getJSONArray("data");
                                if (!dataArray.isEmpty()) {
                                    for (int i = 0; i < dataArray.size(); i++) {
                                        LinkedList<JSONObject> exams = List.of(thisWeekExams, nextWeekExams).get(i);
                                        TreeMap<Integer, JSONArray> sortedTimetable = new TreeMap<>();
                                        dataArray.getJSONObject(i).getJSONObject("timetable").forEach((s, t) -> {
                                            if (t != null)
                                                sortedTimetable.put(Integer.parseInt(s), (JSONArray) t);
                                        });
                                        sortedTimetable.forEach((key, value) -> {
                                            if (key.equals(sortedTimetable.firstKey()))
                                                value.forEach(c -> exams.addFirst((JSONObject) c));
                                            else
                                                value.forEach(c -> exams.addLast((JSONObject) c));
                                        });
                                    }
                                    binding.toggle2.clearChecked();
                                    binding.toggle2.check(R.id.week_18);
                                }
                                break;*/
                            case 3:
                                String term = response.getJSONObject("data").getString("acadYearSemester");
                                getTodayCourses(term);
//                                getExams(term);
                                getWeek(term);
                                remoteViews.setTextViewText(R.id.day, String.format("%s学期\n%s周%s", term, new SimpleDateFormat("MM.dd", Locale.getDefault()).format(new Date()), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));
                                break;
                            case 4:
                                remoteViews.setTextViewText(R.id.week, String.format(context.getString(R.string.week_x), response.getJSONArray("data").getJSONObject(0).getString("weekTimes")));
                                break;
                        }

                        remoteViews.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 0, new Intent(context, AgendaActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                        update(appWidgetManager, appWidgetIds, remoteViews, context);

                    }
                }
            }
        });
        getTerm();
    }

    String getTimePosition(String from, String to) {
        Date now = new Date();
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd hh:mm", Locale.getDefault());
            Date fromDate = simpleDateFormat.parse(from);
            Date toDate = simpleDateFormat.parse(to);
            return now.before(fromDate) ? "after" : now.after(toDate) ? "before" : "in";
        } catch (ParseException _) {
        }
        return "before";
    }

    void getTerm() {
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/");
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist", 3);
    }

    void getWeek(String term) {
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/yd/index/");
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=" + term, 4);
    }

    void getTodayCourses(String term) {
        http.setReferrer(null);
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=" + term, 1);
    }

    void getExams(String term) {
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck" + term, String.format("{\"acadYear\":\"%s\",\"examWeekId\":\"1928284621349085186\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", term), 2);
    }

}
