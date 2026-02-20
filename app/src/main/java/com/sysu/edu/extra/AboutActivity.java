package com.sysu.edu.extra;

import static com.sysu.edu.api.DownloadManager.downloadFile;
import static com.sysu.edu.api.DownloadManager.openFile;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityInfoBinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import io.noties.markwon.Markwon;
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
    Handler handler;
    String path;
    BroadcastReceiver receiver;
    OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityInfoBinding binding = ActivityInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        binding.toolbar.setNavigationOnClickListener(_ -> finishAfterTransition());
        binding.icon.setOnClickListener(_ -> {
            if (click.isEmpty() || System.currentTimeMillis() - click.get(click.size() - 1) < 500) {
                if (click.size() == 4) {
                    params.toast(params.isDeveloper() ? R.string.developer_disabled : R.string.developer_enabled);
                    params.setDeveloper(!params.isDeveloper());
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
                    showUpdateDialog(response);
                    /*try {
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
                    }*/
                }
            }
        };
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()) && intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    params.toast(getString(R.string.download_complete));
                    openFile(AboutActivity.this, path);
                }
            }
        };
        ContextCompat.registerReceiver(this, receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
    }

    public void checkUpdate() {
        okHttpClient.newCall(new Request.Builder().url("https://sysu-tang.github.io/latest.json").build()).enqueue(new Callback() {
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

    void showUpdateDialog(JSONObject response) {
        try {
            int version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            Integer responseVersion = response.getInteger("version");
            if (version < responseVersion) {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + getString(R.string.app_name) + response.getString("versionName") + ".apk";
                String link = response.getString("link");
                AlertDialog updateDialog = new MaterialAlertDialogBuilder(AboutActivity.this)
                        .setMessage("")
                        .setTitle(R.string.higher_version_detected)
                        .setPositiveButton(R.string.download_in_system, (_, _) -> downloadId = ((DownloadManager) getSystemService(DOWNLOAD_SERVICE))
                                .enqueue(new DownloadManager.Request(Uri.parse(link))
                                        .setDestinationUri(Uri.fromFile(new File(path)))
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)))
                        .setNegativeButton(R.string.download_in_browser, (_, _) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))))
                        .setCancelable(response.getBoolean("enforce"))
                        .setNeutralButton(R.string.download_in_app, (_, _) -> downloadFile(AboutActivity.this, link, path))
                        .create();
                updateDialog.show();
                Markwon.builder(AboutActivity.this).build().setMarkdown(Objects.requireNonNull(updateDialog.findViewById(android.R.id.message)), response.getString("description"));
            } else if (version > responseVersion) {
                params.toast(getString(R.string.app_modified_detected));
            }else if(version == responseVersion){
                params.toast(getString(R.string.app_latest_installed));
            }
        } catch (PackageManager.NameNotFoundException _) {
        }
    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }
}


