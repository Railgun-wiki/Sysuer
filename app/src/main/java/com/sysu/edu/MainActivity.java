package com.sysu.edu;

import static com.sysu.edu.api.DownloadManager.downloadFile;
import static com.sysu.edu.api.DownloadManager.openFile;

import android.app.DownloadManager;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.RemoteViews;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.sysu.edu.academic.AcademyNotification;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.academic.AssistantEvaluationActivity;
import com.sysu.edu.academic.AssistantInfoActivity;
import com.sysu.edu.academic.CETActivity;
import com.sysu.edu.academic.CalendarActivity;
import com.sysu.edu.academic.ClassroomQueryActivity;
import com.sysu.edu.academic.CourseCompletionActivity;
import com.sysu.edu.academic.CourseQueryActivity;
import com.sysu.edu.academic.CourseScheduleActivity;
import com.sysu.edu.academic.CourseSelectedActivity;
import com.sysu.edu.academic.CourseSelectionActivity;
import com.sysu.edu.academic.DormActivity;
import com.sysu.edu.academic.EvaluationActivity;
import com.sysu.edu.academic.ExamActivity;
import com.sysu.edu.academic.GradeActivity;
import com.sysu.edu.academic.GradeForLevelActivity;
import com.sysu.edu.academic.LeaveReturnRegistrationActivity;
import com.sysu.edu.academic.MajorInfo;
import com.sysu.edu.academic.PersonalInformationActivity;
import com.sysu.edu.academic.PhysicalFitnessTestResultActivity;
import com.sysu.edu.academic.RegisterInfoActivity;
import com.sysu.edu.academic.RoomQueryActivity;
import com.sysu.edu.academic.SchoolEnrollmentActivity;
import com.sysu.edu.academic.SchoolWorkWarning;
import com.sysu.edu.academic.TrainingProgramActivity;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.PreferenceViewModel;
import com.sysu.edu.browser.BrowserActivity;
import com.sysu.edu.databinding.ActivityMainBinding;
import com.sysu.edu.home.HomeViewModel;
import com.sysu.edu.life.GymReservationActivity;
import com.sysu.edu.life.NetPayActivity;
import com.sysu.edu.life.NewsActivity;
import com.sysu.edu.life.Pay;
import com.sysu.edu.life.SchoolBusActivity;
import com.sysu.edu.studentAffair.StudentPartTimeActivity;
import com.sysu.edu.todo.TodoActivity;
import com.sysu.edu.widget.NextClassWidget;
import com.sysu.edu.widget.NextClassWidgetWorker;
import com.sysu.edu.widget.TodayClassWidget;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.noties.markwon.Markwon;

public class MainActivity extends AppCompatActivity {

    final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            _ -> {
            }
    );
    long downloadId;
    //    ActivityResultLauncher<Intent> detailLauncher;
    BroadcastReceiver receiver;
    Params params;
    HttpManager http;
    String path;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        initActionMap(viewModel.actionMap);
        NavHostFragment fragment = (NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.dashboard_scroll));
        NavController navController = fragment.getNavController();
        NavGraph graph = new NavInflater(this, navController.getNavigatorProvider()).inflate(R.navigation.main_nav);
        graph.setStartDestination(new int[]{R.id.navigation_dashboard, R.id.navigation_service, R.id.navigation_account}[Integer.parseInt(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getString("home", "0"))]);
//        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
//            //Log.d(TAG, "Shizuku 权限已授予");
//        } else {
//            Shizuku.requestPermission(0);
//            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
//                if (grantResult == PackageManager.PERMISSION_GRANTED) {
//                    // 去连接服务（前提是Shizuku服务是正常并且已授权）
//                    Shizuku.bindUserService(new Shizuku.UserServiceArgs(new ComponentName("com.sysu.edu", UserService.class.getName()))
//                            .daemon(false)
//                            .processNameSuffix("service")
//                            .debuggable(BuildConfig.DEBUG)
//                            .version(2024), new ServiceConnection() {
//                        @Override
//                        public void onServiceConnected(ComponentName componentName, IBinder binder) {
//                            if (binder != null && binder.pingBinder()) {
//                                IUserService mUserService = IUserService.Stub.asInterface(binder);
//
//                                // executeWifiCommand();
//                            } else {
//                                //Log.i(TAG, " Shizuku binder 为 null 或者 binder.pingBinder() 有问题");
//                            }
//                        }
//
//                        @Override
//                        public void onServiceDisconnected(ComponentName componentName) {
//
//                        }
//                    });
//                    //Toast.makeText(MainActivity.this, "授权成功", Toast.LENGTH_SHORT).show();
//                } else {
//                    //Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
        navController.setGraph(graph);
        NavigationUI.setupWithNavController((NavigationBarView) binding.navView, navController);

        PreferenceViewModel spm = new ViewModelProvider(this).get(PreferenceViewModel.class);
        spm.setPM(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this));
        spm.initLiveData();
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(R.string.user_agreement_and_privacy_policy)
                .setMessage("")
                .setPositiveButton(R.string.agree, (_, _) -> {
                    spm.setIsAgree(true);
                    spm.setIsAgreeLiveData(false);
                })
                .setNegativeButton(R.string.disagree, (_, _) -> {
                    spm.setIsAgree(false);
                    supportFinishAfterTransition();
                })
                .setCancelable(false)
                .create();
        spm.getIsAgreeLiveData().observe(this, aBoolean -> {
            if (aBoolean) {
                dialog.show();
                Markwon.builder(this).build().setMarkdown(Objects.requireNonNull(dialog.findViewById(android.R.id.message)), "请阅读[用户协议](https://sysu-tang.github.io/sysuer-website/docs/userAgreement)和[隐私政策](https://sysu-tang.github.io/sysuer-website/docs/privacyPolicy)");
            } else if (spm.getUpdate()) {
                checkUpdate();
            }
        });
        spm.setIsFirstLaunch(false);
        params = new Params(this);
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    showUpdateDialog(response);
                }
            }
        });
        http.setParams(params);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()) && intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    params.toast(getString(R.string.download_complete));
                    openFile(MainActivity.this, path);
                }
            }
        };
        ContextCompat.registerReceiver(this, receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
//        detailLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), _ -> {
//        });
        //PackageManager pm = getPackageManager();
        //pm.setComponentEnabledSetting(new ComponentName(this, SettingActivity.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        //WorkManager.getInstance(this).enqueue(new OneTimeWorkRequest.Builder(ClassIsland.class).build());
        //new ClassIsland(this).doWork();
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        } else {
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intentMorning = new Intent(this, AlarmReceiver.class).setAction("VIDEO_TIMER");
            PendingIntent piMorning = PendingIntent.getBroadcast(this, 0, intentMorning, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);     //设置事件
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    System.currentTimeMillis() + 2 * 1000, piMorning);
        }*/
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, PackageManager.PERMISSION_GRANTED);
            }
        }
        /*handler.postAtTime(() -> {
            ClassIsland.sendCourseNotification(
                    this,
                    "高等数学",           // 课程名称
                    "9分钟",            // 剩余时间
                    "逸夫楼301"          // 教室
            );
        },  SystemClock.uptimeMillis() + 4 * 1000);*/
//        Intent service = new Intent(this, CourseService.class);
//        ContextCompat.startForegroundService(this, service);
    }

    void showUpdateDialog(JSONObject response) {
        try {
            int version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            Integer responseVersion = response.getInteger("version");
            if (version < responseVersion) {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + getString(R.string.app_name) + response.getString("versionName") + ".apk";
                AlertDialog updateDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                        .setMessage("")
                        .setTitle(R.string.higher_version_detected)
                        .setPositiveButton(R.string.download_in_system, (_, _) -> downloadId = ((DownloadManager) getSystemService(DOWNLOAD_SERVICE))
                                .enqueue(new DownloadManager.Request(Uri.parse(response.getString("link")))
                                        .setDestinationUri(Uri.fromFile(new File(path)))
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)))
                        .setNegativeButton(R.string.download_in_browser, (_, _) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(response.getString("link")))))
                        .setCancelable(response.getBoolean("enforce"))
                        .setNeutralButton(R.string.download_in_app, (_, _) -> downloadFile(MainActivity.this, response.getString("link"), path))
                        .create();
                updateDialog.show();
                Markwon.builder(MainActivity.this).build().setMarkdown(Objects.requireNonNull(updateDialog.findViewById(android.R.id.message)), response.getString("description"));
            } /*else if (version > responseVersion) {
                params.toast(getString(R.string.app_modfied_detected));
            }else {
                params.toast(getString(R.string.app_latest_installed));
            }*/
        } catch (PackageManager.NameNotFoundException _) {
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(new ComponentName(this, NextClassWidget.class), new RemoteViews(getPackageName(), R.layout.widget_next_class));
        appWidgetManager.updateAppWidget(new ComponentName(this, TodayClassWidget.class), new RemoteViews(getPackageName(), R.layout.widget_today_class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PackageManager.PERMISSION_GRANTED) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                params.toast(R.string.permission_granted);
            }
        }
    }

    void beginClassNotificationWorker(Date target) {
        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork("next_class_widget_update",
                        ExistingWorkPolicy.KEEP, new OneTimeWorkRequest.Builder(NextClassWidgetWorker.class).setInitialDelay(target.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS).build());


    }

    void checkUpdate() {
        http.getRequest("https://sysu-tang.github.io/latest.json", 0);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        receiver = null;
        super.onDestroy();
    }

    View.OnClickListener browse(String url) {
        return view -> startActivity(new Intent(this, BrowserActivity.class).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, "miniapp").toBundle());
    }

    View.OnClickListener newActivity(Class<?> activity_class) {
        return view -> launcher.launch(new Intent(this, activity_class), ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, "miniapp"));
    }

    void initActionMap(Map<Integer, View.OnClickListener> actionMap) {
        // 学术服务 (id: 1xx)
        actionMap.put(101, newActivity(SchoolEnrollmentActivity.class));           // 学籍
        actionMap.put(102, newActivity(CETActivity.class));          // 四六级
        actionMap.put(103, newActivity(RegisterInfoActivity.class));         // 注册
        actionMap.put(104, newActivity(SchoolWorkWarning.class));    // 学业预警
        actionMap.put(105, newActivity(CourseCompletionActivity.class));     // 课程完成情况
        actionMap.put(106, newActivity(LeaveReturnRegistrationActivity.class));     // 请假返回登记
        actionMap.put(107, newActivity(PhysicalFitnessTestResultActivity.class));     // 体测
        actionMap.put(108, newActivity(DormActivity.class));     // 宿舍
        actionMap.put(109, newActivity(PersonalInformationActivity.class));     // 个人信息
        actionMap.put(110, newActivity(StudentPartTimeActivity.class));     // 勤工俭学


        // 学习服务 (id: 2xx)
        actionMap.put(201, newActivity(TodoActivity.class));         // 待办
//        actionMap.put(202, browse("https://explore.sysu.edu.cn/"));         // 交叉探索平台
//        actionMap.put(203, browse("https://aic.sysu.edu.cn/"));         // 逸仙智课平台
        actionMap.put(202, newActivity(AgendaActivity.class));         // 日程


        // 资讯门户 (id: 3xx)
        actionMap.put(301, newActivity(NewsActivity.class));                 // 资讯门户
        actionMap.put(302, _ -> {
            try {
                startActivity(Objects.requireNonNull(getPackageManager().getLaunchIntentForPackage("com.comingx.zanao")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException e) {
                params.toast(R.string.no_app);
            }
        }); // 校园集市
        actionMap.put(303, newActivity(AcademyNotification.class));  // 教务通知

        // 系统服务 (id: 4xx)
//        actionMap.put(401, browse("https://gym.sysu.edu.cn/#/"));                   // 体育场馆预定系统
//        actionMap.put(402, browse("https://xgxt.sysu.edu.cn/main/#/index"));        // 学工系统
//        actionMap.put(403, browse("https://jwxt.sysu.edu.cn/jwxt/yd/index/#/Home"));           // 本科教务系统
//        actionMap.put(404, browse("https://portal.sysu.edu.cn/newClient/#/newPortal/index"));  // 中山大学统一门户
//        actionMap.put(405, browse("https://usc.sysu.edu.cn/taskcenter-v4/workflow/index"));    // 大学服务中心
//        actionMap.put(406, browse("https://cwxt.webvpn.sysu.edu.cn/#/home/index"));        // 财务信息系统

        // 官网服务 (id: 5xx)
//        actionMap.put(501, browse("https://www.sysu.edu.cn/"));              // 中山大学官网
//        actionMap.put(502, browse("https://admission.sysu.edu.cn/"));        // 本科招生
//        actionMap.put(503, browse("https://graduate.sysu.edu.cn/zsw/"));     // 研究生招生
//        actionMap.put(504, browse("https://rcb.sysu.edu.cn/"));              // 人才招聘
//        actionMap.put(505, browse("https://sysu100.sysu.edu.cn/"));          // 百年校庆
//        actionMap.put(506, browse("https://bwgxsg.sysu.edu.cn/"));           // 博物馆
//        actionMap.put(507, browse("https://library.sysu.edu.cn/"));          // 图书馆
//        actionMap.put(508, browse("https://alumni.sysu.edu.cn/"));           // 校友会
//        actionMap.put(509, browse("https://mail.sysu.edu.cn/"));             // 公务电子邮件系统

        // 官方服务 (id: 6xx)
        actionMap.put(601, _ -> {    // 二维码
            String linking = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getString("qrcode", "");
            if (!linking.isEmpty()) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(linking)));
                } catch (ActivityNotFoundException e) {
                    params.toast(R.string.no_app);
                }
            }/* else {
                //new LaunchMiniProgram(this).launchMiniProgram("gh_85575b9f544e");
            }*/
        });
        actionMap.put(602, _ -> {
            try {
                startActivity(Objects.requireNonNull(this.getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                //(requireContext(), R.string.no_app, Toast.LENGTH_LONG).show();
            }
        }); // 企业微信
        //actionMap.put(603, v -> startActivity(Objects.requireNonNull(this.getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))); // 中大招生

        // 教务服务 (id: 7xx)
        actionMap.put(701, newActivity(EvaluationActivity.class));           // 评教
        actionMap.put(702, newActivity(CourseSelectionActivity.class));
        actionMap.put(703, newActivity(CourseScheduleActivity.class));               // 课程表
        actionMap.put(704, newActivity(ExamActivity.class));                 // 考试
        actionMap.put(705, newActivity(CalendarActivity.class));             // 校历
        actionMap.put(706, newActivity(ClassroomQueryActivity.class));       // 自习室
        actionMap.put(707, newActivity(GradeActivity.class));                        // 成绩
        actionMap.put(708, newActivity(CourseQueryActivity.class));                  // 课程
//        actionMap.put(709, browse("https://jwxt.sysu.edu.cn/jwxt/mk/#/personalTrainingProgramView")); // 个人培养方案
        actionMap.put(710, newActivity(TrainingProgramActivity.class));             // 培养方案
        actionMap.put(711, newActivity(MajorInfo.class));                    // 专业
        actionMap.put(712, newActivity(CourseSelectedActivity.class));                  // 已选课程
        actionMap.put(713, newActivity(AssistantInfoActivity.class));       // 助教信息
        actionMap.put(714, newActivity(GradeForLevelActivity.class));           // 等级制成绩
        actionMap.put(715, newActivity(RoomQueryActivity.class));           // 教室
        actionMap.put(716, newActivity(AssistantEvaluationActivity.class)); // 助教考核


        // 学习平台 (id: 8xx)
//        actionMap.put(801, browse("http://seelight.net/html/homePage/homePagePhone.html"));             // SeeLight
//        actionMap.put(802, browse("https://www.yuketang.cn/web"));           // 雨课堂
//        actionMap.put(803, browse("https://www.ketangpai.com/"));            // 课堂派
//        actionMap.put(804, browse("https://lms.sysu.edu.cn/"));              // 在线教学平台
//        actionMap.put(805, browse("https://www.icourse163.org/"));           // 中国大学（慕课）
//        actionMap.put(806, browse("https://welearn.sflep.com/index.aspx"));  // WeLearn
//        actionMap.put(807, browse("https://www.pigai.org/"));              // 批改网

        // 生活服务 (id: 9xx)
        actionMap.put(902, newActivity(SchoolBusActivity.class));                    // 校车
//        actionMap.put(903, browse("https://visitor.sysu.edu.cn/"));                 // 逸仙通行
//        actionMap.put(905, browse("https://gongfang.sysu.edu.cn/h5_separation/repair_apply/index.html#/applyDetail/20251231162524362223"));                 // 报修
//        actionMap.put(906, browse("https://zhny.sysu.edu.cn/h5/#/"));        // 水电费
        actionMap.put(907, newActivity(Pay.class));                          // 缴费大厅
        actionMap.put(908, newActivity(GymReservationActivity.class));     // 体育馆预约
        actionMap.put(909, newActivity(NetPayActivity.class));              // 校园网


        // 人工智能服务 (id: 10xx)
//        actionMap.put(1001, browse("https://chat.sysu.edu.cn/zntgc/agent"));     // Deepseek
//        actionMap.put(1002, browse("https://chat.sysu.edu.cn/znt/chat/empty"));  // 逸闻
//        actionMap.put(1003, browse("https://xgxw.sysu.edu.cn/aicounsellor/agents/outlink/sunyatsenuniversity")); // 学工君
    }
}