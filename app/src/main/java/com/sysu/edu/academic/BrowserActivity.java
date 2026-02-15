package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.trim;
import static com.sysu.edu.api.DownloadManager.downloadFile;
import static com.sysu.edu.login.LoginManager.initWeb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityBrowserBinding;
import com.sysu.edu.databinding.DialogGridBinding;
import com.sysu.edu.databinding.DialogJsBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.extra.JavaScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BrowserActivity extends AppCompatActivity {
    WebView web;
    ActivityBrowserBinding binding;
    WebSettings webSettings;
    CookieManager cookie;
    MutableLiveData<Integer> progress = new MutableLiveData<>();
    MaterialButton refreshButton;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> finishAfterTransition());
        Params params = new Params(this);
        StringBuilder result = new StringBuilder();
        String url = getIntent().getDataString() != null ? getIntent().getDataString() : "https://www.sysu.edu.cn/";
        try {
            InputStreamReader input = new InputStreamReader(getAssets().open("js.json"));
            BufferedReader buffer = new BufferedReader(input);
            String line;
            while ((line = buffer.readLine()) != null) result.append(line);
            input.close();
            buffer.close();
        } catch (IOException _) {}
        JavaScript js = new JavaScript(result.toString());
        web = binding.web;
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url1 = String.valueOf(request.getUrl());
//                if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/system-manage/infoRelease/downloadFile", Pattern.DOTALL).matcher(url1).find()) {
//                    view.loadUrl(url1,Map.of("Cookie", cookie.getCookie(url1), "Referer", "https://jwxt.sysu.edu.cn/jwxt/"));
//                }else{
                view.loadUrl(url1);
//                }
                return true;
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url1 = request.getUrl().toString();
                if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/system-manage/infoRelease/downloadFile", Pattern.DOTALL).matcher(url1).find()) {
                    try {
                        Response response = new OkHttpClient().newCall(new Request.Builder().url(url1)
                                .header("Cookie", cookie.getCookie(url1))
                                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/")
                                .build()).execute();
                        MediaType mediaType = response.body().contentType();
                        return new WebResourceResponse(mediaType == null ? "application/octet-stream" : mediaType.type(), "utf-8", response.body().byteStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String link) {
                if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/esc-sso/login/page").matcher(link).find()) {
                    web.evaluateJavascript(String.format("""
                            javascript:(function(){\
                            function waitElement(selector, callback) {\
                            const element = document.querySelector(selector);\
                            if (element) {callback();}else{setTimeout(() => {waitElement(selector,callback);}, 100);}}\
                            waitElement('.para-widget-account-psw', () => {\
                            var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click();});})()""", params.getAccount(), params.getPassword()), _ -> {
                    });
                } else if (Pattern.compile("://appgw.sysu.edu.cn/").matcher(link).find()) {
                    web.stopLoading();
                    web.loadUrl(url.replace(".sysu.edu.cn/", "-443.webvpn.sysu.edu.cn/"));
                } else {
                    view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
                }
                super.onPageFinished(view, link);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new MaterialAlertDialogBuilder(BrowserActivity.this).setMessage(message).setPositiveButton(R.string.confirm, (_, _) -> result.confirm()).create().show();
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new MaterialAlertDialogBuilder(BrowserActivity.this).setMessage(message).setPositiveButton(R.string.confirm, (_, _) -> result.confirm()).create().show();
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(BrowserActivity.this);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        web.loadUrl(String.valueOf(request.getUrl()));
                        newWebView.destroy();
                        return super.shouldOverrideUrlLoading(view, request);
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progress.postValue(newProgress);
            }
        });
        web.setDownloadListener((url1, _, _, _, _) -> {
            System.out.println(url1);
//            System.out.println(cookie.getCookie(url1));
            String fileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + Uri.parse(url1).getQueryParameter("fileName");
            params.toast("下载开始,文件将保存到" + fileName);
            downloadFile(BrowserActivity.this, new Request.Builder()
                    .url(url1)
                    .header("Cookie", cookie.getCookie(url1))
                    .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/").build(), fileName);
        });
        progress.observe(this, p -> {
            if (p == 100) {
                refreshButton.setIconResource(R.drawable.refresh);
                refreshButton.setText(R.string.refresh);
            } else {
                refreshButton.setIconResource(R.drawable.close);
                refreshButton.setText(R.string.stop);
            }
        });
        BottomSheetDialog jsDialog = new BottomSheetDialog(BrowserActivity.this);
        DialogJsBinding JSBinding = DialogJsBinding.inflate(getLayoutInflater());
        jsDialog.setContentView(JSBinding.getRoot());
        jsDialog.setTitle(R.string.js);
        RecyclerView recyclerView = JSBinding.recyclerView.getRoot();
        JSAdapter jsAdapter = new JSAdapter(web);
        recyclerView.setLayoutManager(new LinearLayoutManager(BrowserActivity.this));
        recyclerView.setAdapter(jsAdapter);

        BottomSheetDialog menuDialog = new BottomSheetDialog(BrowserActivity.this);
        DialogGridBinding menuBinding = DialogGridBinding.inflate(getLayoutInflater());
        List<Integer> menuTitle = List.of(R.string.back, R.string.forward, R.string.refresh, R.string.exit, R.string.page_up, R.string.page_down, R.string.zoom_in, R.string.zoom_out);
        List<Integer> menuIcon = List.of(R.drawable.left, R.drawable.right, R.drawable.refresh, R.drawable.exit, R.drawable.up, R.drawable.down, R.drawable.zoom_in, R.drawable.zoom_out);
        List<Consumer<MaterialButton>> menuAction = List.of(_ -> goBack(), _ -> goForward(), _ -> refresh(), _ -> supportFinishAfterTransition(), _ -> pageUp(), _ -> pageDown()
                , _ -> web.zoomIn(), _ -> web.zoomOut());
        int[] referenceIds = loadMenu(menuBinding, menuTitle, menuIcon, menuAction);
        menuDialog.setContentView(menuBinding.getRoot());

        refreshButton = menuBinding.getRoot().findViewById(referenceIds[2]);
        binding.js.setOnClickListener(_ -> {
            jsAdapter.setJS(js.searchJS(trim(web.getUrl())));
            jsDialog.show();
        });
        binding.menu.setOnClickListener(_ -> {
            menuBinding.grid.setReferencedIds(referenceIds);
            menuDialog.show();
        });

        cookie = CookieManager.getInstance();
        Menu menu = binding.toolbar.getMenu();
        menu.add(R.string.open_in_browser).setOnMenuItemClickListener(_ -> {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(trim(web.getUrl()))));
            return false;
        });
        menu.add(R.string.refresh).setOnMenuItemClickListener(_ -> {
            web.reload();
            return false;
        });
        menu.add(R.string.clear_cookie).setOnMenuItemClickListener(_ -> {
            cookie.removeAllCookies(_ -> {
            });
            cookie.flush();
            return false;
        });
        menu.add(R.string.exit).setOnMenuItemClickListener(_ -> {
            supportFinishAfterTransition();
            return false;
        });
        binding.back.setOnClickListener(_ -> {
            if (web.canGoBack()) web.goBack();
        });
        binding.forward.setOnClickListener(_ -> {
            if (web.canGoForward()) web.goForward();
        });
        initWeb(web);
        if (getIntent().hasExtra("data") && getIntent().getStringExtra("data") != null) {
            webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36");
            web.loadDataWithBaseURL("https://jwxt.sysu.edu.cn", Objects.requireNonNull(getIntent().getStringExtra("data")), "text/html", "utf-8", "https://jwxt.sysu.edu.cn");
        } else {
            web.loadUrl(url);
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (progress.getValue() != null && progress.getValue() != 100) {
                    web.stopLoading();
                } else if (web.canGoBack()) {
                    web.goBack();
                } else {
                    supportFinishAfterTransition();
                }
            }
        });
    }

    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            ((ViewGroup) web.getParent()).removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    int[] loadMenu(DialogGridBinding binding, List<Integer> menuTitle, List<Integer> menuIcon, List<Consumer<MaterialButton>> menuAction) {
        ArrayList<Integer> ids = new ArrayList<>();
        IntStream.range(0, menuTitle.size()).forEach(i -> {
            MaterialButton menu = new MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle);
            menu.setText(menuTitle.get(i));
            menu.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_TOP);
            if (menuIcon.size() > i && menuIcon.get(i) != 0)
                menu.setIconResource(menuIcon.get(i));
            int id = View.generateViewId();
            ids.add(id);
            menu.setId(id);
            if (menuAction.size() > i && menuAction.get(i) != null)
                menu.setOnClickListener(_ -> menuAction.get(i).accept(menu));
            binding.getRoot().addView(menu);
        });
        int[] referenceIds = ids.stream().mapToInt(Integer::intValue).toArray();
        binding.grid.setReferencedIds(referenceIds);
        binding.grid.setRows((menuTitle.size() + 3) / 4);
        return referenceIds;
    }

    void goBack() {
        if (web.canGoBack()) web.goBack();
    }

    void goForward() {
        if (web.canGoForward()) web.goForward();
    }

    void refresh() {
        if (progress.getValue() != null && progress.getValue() == 100) {
            web.reload();
        } else {
            web.stopLoading();
        }
    }

    void pageUp() {
        web.pageUp(true);
    }

    void pageDown() {
        web.pageDown(true);
    }

    static class JSAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final WebView web;
        ArrayList<JSONObject> data = new ArrayList<>();

        public JSAdapter(WebView web) {
            super();
            this.web = web;
        }

        public void setJS(ArrayList<JSONObject> list) {
            clear();
            data = list;
            notifyItemRangeInserted(0, getItemCount());
        }

        public void clear() {
            int size = getItemCount();
            data.clear();
            notifyItemRangeRemoved(0, size);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preference, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemPreferenceBinding binding = ItemPreferenceBinding.bind(holder.itemView);
            binding.itemTitle.setText(data.get(position).getString("title"));
            binding.itemContent.setText(data.get(position).getString("description"));
            binding.itemIcon.setImageResource(R.drawable.js);
            binding.getRoot().updateAppearance(position, getItemCount());
            binding.getRoot().setOnClickListener(_ -> web.evaluateJavascript(data.get(position).getString("script"), _ -> {
            }));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

}