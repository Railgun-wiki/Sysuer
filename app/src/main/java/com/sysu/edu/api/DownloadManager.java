package com.sysu.edu.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.sysu.edu.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager {

    /**
     * 下载网络文件到指定路径
     *
     * @param context 上下文对象
     * @param url     网络文件 URL
     * @param path    本地文件保存路径
     */
    public static void downloadFile(Activity context, String url, String path) {
        downloadFile(context, new Request.Builder().url(url).build(), path);
    }

    /**
     * 下载网络文件到指定路径
     *
     * @param context 上下文对象
     * @param request 网络请求对象
     * @param path    本地文件保存路径
     */
    public static void downloadFile(Activity context, Request request, String path) {
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("下载网络文件报错：" + e.getMessage());
                context.runOnUiThread(() -> Toast.makeText(context, "下载网络文件报错：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) {
                MediaType type = response.body().contentType();
                String mediaType = type == null ? "application/octet-stream" : type.toString();
                long length = response.body().contentLength();
                System.out.println("网络文件信息：" + String.format(Locale.getDefault(), "文件类型为%s，文件大小为%d", mediaType, length));
                System.out.println("下载网络文件到：" + path);
                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(path)) {
                    byte[] buf = new byte[100 * 1024];
                    int sum = 0, len;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        String detail = String.format(Locale.getDefault(), "已下载%.2fKB", sum / 1024.0f);
//                        System.out.println("下载进度：" + detail);
                    }
                    is.close();
                    fos.close();
                    System.out.println("下载完成");
                    openFile(context, path);
                } catch (Exception _) {
                }
            }
        });
    }

    /**
     * 打开文件
     *
     * @param context 上下文对象
     * @param path    文件路径
     */
    public static void openFile(Context context, String path) {
        context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW)
                        .addCategory("android.intent.category.DEFAULT")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setDataAndType(FileProvider.getUriForFile(context, "com.sysu.edu.fileProvider", new File(path)), MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substring(path.lastIndexOf(".") + 1).toLowerCase())),
                context.getString(R.string.share)
        ));
    }
}
