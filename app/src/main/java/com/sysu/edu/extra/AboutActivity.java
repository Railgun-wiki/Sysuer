package com.sysu.edu.extra;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityInfoBinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("ALL")
public class AboutActivity extends AppCompatActivity {

    final ArrayList<Long> click = new ArrayList<>();
    Params params;
    File file;
    long downloadId;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityInfoBinding binding = ActivityInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        binding.toolbar.setNavigationOnClickListener(_ -> finishAfterTransition());
        binding.icon.setOnClickListener(_ -> {
            //noinspection SequencedCollectionMethodCanBeUsed
            if (click.isEmpty() || System.currentTimeMillis() - click.get(click.size() - 1) < 500) {
                if (click.size() == 4) {
                    params.toast("已开启开发者模式");
                    getPreferences(Context.MODE_PRIVATE).edit().putBoolean("developer", true).apply();
                    click.clear();
                } else {
                    click.add(System.currentTimeMillis());
                }
            } else {
                click.clear();
            }
        });
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    try {
                        int version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                        if (version < response.getInteger("version")) {
                            new MaterialAlertDialogBuilder(AboutActivity.this).setMessage(response.getString("description")).setTitle("发现新版本").setPositiveButton("更新", (_, _) -> {
                                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sysuer.apk");
                                downloadId = ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(new DownloadManager.Request(Uri.parse(response.getString("link"))).setDestinationUri(Uri.fromFile(file)).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED));
                            }).setNegativeButton(R.string.cancel, (_, _) -> {
                            }).setCancelable(response.getBoolean("enforce")).create().show();
                        } else if (version < response.getInteger("version")) {
                            params.toast("本APP已被篡改");
                        } else {
                            params.toast("已为最新版本");
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                    if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                        params.toast("完成下载");
                        startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(context, getPackageName() + ".fileProvider", file), "application/*"));
                    }
                }
            }
        };
        ContextCompat.registerReceiver(this, receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    public void checkUpdate() {
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://sysu-tang.github.io/latest.json").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.sendEmptyMessage(-1);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 0;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}


