package com.sysu.edu.browser;

import static com.sysu.edu.api.CommonUtil.trim;
import static com.sysu.edu.api.DownloadManager.downloadFile;
import static com.sysu.edu.api.FileManager.readAssets;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityBrowserBinding;
import com.sysu.edu.databinding.DialogJsBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.template.RecyclerAdapter;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.EditTextDialog;
import com.sysu.edu.view.GridDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
    BrowserHelper db;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = new BrowserHelper(this);
        binding.toolbar.setNavigationOnClickListener(_ -> finishAfterTransition());
        Params params = new Params(this);
        BrowserPreference preference = new BrowserPreference(this);
        String url = getIntent().getDataString() != null ? getIntent().getDataString() : "https://www.sysu.edu.cn/";
        JavaScript js = new JavaScript(readAssets(this, "js.json"));
        web = binding.web;
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url1 = String.valueOf(request.getUrl());
                if (url1.startsWith("https://") || url1.startsWith("http://")) view.loadUrl(url1);
                else
                    startActivity(new Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setData(Uri.parse(url1)));
                return true;
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url1 = String.valueOf(request.getUrl());
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
                            var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click();});})()""", params.getUserName(), params.getPassword()), _ -> {
                    });
                } else if (Pattern.compile("://appgw.sysu.edu.cn/").matcher(link).find()) {
                    web.stopLoading();
                    web.loadUrl(url.replace(".sysu.edu.cn/", "-443.webvpn.sysu.edu.cn/"));
                } else if (preference.isPC()) {
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
        webSettings = web.getSettings();
        webSettings.supportZoom();
        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptEnabled(preference.isJSEnabled());
        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(preference.isSaveMobileDataMode() ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(preference.isImageBlocked());
        webSettings.setDefaultTextEncodingName("utf-8");
//        webSettings = web.getSettings();

        /*
         * 脚本弹窗
         * */
        BottomSheetDialog jsDialog = new BottomSheetDialog(BrowserActivity.this);
        DialogJsBinding JSBinding = DialogJsBinding.inflate(getLayoutInflater());
        jsDialog.setContentView(JSBinding.getRoot());
        jsDialog.setTitle(R.string.js);
        JSAdapter jsAdapter = new JSAdapter();
        jsAdapter.setListener(new AdapterListener() {
            @Override
            public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setOnClickListener(_ -> web.evaluateJavascript(jsAdapter.get(position).getString("script"), null));
            }

            @Override
            public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
            }
        });

        RecyclerView jsList = JSBinding.recyclerView.getRoot();
        jsList.setLayoutManager(new LinearLayoutManager(BrowserActivity.this));
        jsList.setAdapter(jsAdapter);
        JSBinding.manage.setOnClickListener(v -> startActivity(new Intent(BrowserActivity.this, JSActivity.class),
                ActivityOptionsCompat.makeSceneTransitionAnimation(BrowserActivity.this, v, "miniapp").toBundle()));


        /*
         * 菜单弹窗
         * */
        GridDialog menuDialog = new GridDialog(this);
        List<Integer> menuTitle = List.of(R.string.back, R.string.forward, R.string.refresh, R.string.exit, R.string.page_up, R.string.page_down, R.string.zoom_in, R.string.zoom_out);
        List<Integer> menuIcon = List.of(R.drawable.left, R.drawable.right, R.drawable.refresh, R.drawable.exit, R.drawable.up, R.drawable.down, R.drawable.zoom_in, R.drawable.zoom_out);
        List<Consumer<MaterialButton>> menuAction = List.of(_ -> goBack(), _ -> goForward(), _ -> refresh(), _ -> supportFinishAfterTransition(), _ -> pageUp(), _ -> pageDown()
                , _ -> web.zoomIn(), _ -> web.zoomOut());
        menuDialog.loadMenu(menuTitle, menuIcon, menuAction, Integer.class);
        refreshButton = menuDialog.getMenu(2);

        /*
         * UA 弹窗
         * */
        GridDialog uaDialog = new GridDialog(this);
        uaDialog.setColumn(2);
        uaDialog.setSelectable(true);
        Cursor cursor = db.getReadableDatabase().query("ua", null, null, null, null, null, null);
        ArrayList<String> uaNames = new ArrayList<>();
        ArrayList<Integer> uaIcons = new ArrayList<>();
        ArrayList<Consumer<MaterialButton>> uaAction = new ArrayList<>();
        uaNames.add(getString(R.string.follow_system));
        uaIcons.add(R.drawable.setting);
        uaAction.add(_ -> {
            this.webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(this));
            preference.setUA(0);
            web.reload();
        });
        if (cursor.moveToFirst()) {
            do {
                uaNames.add(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                int uaId = cursor.getInt(cursor.getColumnIndexOrThrow("uaId"));
                uaIcons.add(List.of(R.drawable.laptop, R.drawable.laptop, R.drawable.laptop, R.drawable.mac, R.drawable.android, R.drawable.tablet, R.drawable.iphone, R.drawable.ipad, R.drawable.ua, R.drawable.laptop, R.drawable.laptop, R.drawable.android).get(uaId));
                String ua = cursor.getString(cursor.getColumnIndexOrThrow("ua"));
                uaAction.add(_ -> {
                    this.webSettings.setUserAgentString(ua);
                    preference.setUA(uaId);
                    web.reload();
                });
            } while (cursor.moveToNext());
        }
        cursor.close();
        uaDialog.loadMenu(uaNames, uaIcons, uaAction, String.class);
        uaDialog.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        uaDialog.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        uaDialog.selectMenu(preference.getUA());

        /*
         * 网页弹窗
         * */
        GridDialog browserDialog = new GridDialog(this);
        List<Integer> webTitle = List.of(R.string.ua, preference.isPC() ? R.string.pc_mode : R.string.mobile_mode, preference.isImageBlocked() ? R.string.image_blocked : R.string.image, R.string.javascript, R.string.save_mobile_data_mode);
        List<Integer> webIcon = List.of(R.drawable.ua, preference.isPC() ? R.drawable.laptop : R.drawable.phone, preference.isImageBlocked() ? R.drawable.image_block : R.drawable.image, R.drawable.js, R.drawable.wifi);
        List<Consumer<MaterialButton>> webAction = List.of(_ -> uaDialog.show(), v -> {
                    boolean pc = !preference.isPC();
                    preference.setPC(pc);
                    v.setText(pc ? R.string.pc_mode : R.string.mobile_mode);
                    v.setIconResource(pc ? R.drawable.laptop : R.drawable.phone);
                    web.reload();
                },
                v -> {
                    boolean imageBlocked = !preference.isImageBlocked();
                    preference.setImageBlocked(imageBlocked);
                    v.setText(imageBlocked ? R.string.image_blocked : R.string.image);
                    v.setIconResource(imageBlocked ? R.drawable.image_block : R.drawable.image);
                    webSettings.setBlockNetworkImage(imageBlocked);
                },
                _ -> {
                    boolean jsEnabled = !preference.isJSEnabled();
                    preference.setJSEnabled(jsEnabled);
                    webSettings.setJavaScriptEnabled(jsEnabled);
                }, v -> {
                    boolean saveMobileDataMode = !preference.isSaveMobileDataMode();
                    preference.setSaveMobileDataMode(saveMobileDataMode);
                    v.setIconResource(saveMobileDataMode ? R.drawable.no_wifi : R.drawable.wifi);
                    webSettings.setCacheMode(saveMobileDataMode ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
                });
        browserDialog.loadMenu(webTitle, webIcon, webAction, Integer.class);
        browserDialog.setTogglable(new int[]{3, 4}, true);
        browserDialog.setColumn(4);
        browserDialog.toggleMenu(3, true);

        /*
         * Cookie 弹窗
         * */
        var cookieDialog = new EditTextDialog(this);
        cookieDialog.setTitle(R.string.cookie);

        /*
         * 网站弹窗
         * */
        var websiteDialog = new GridDialog(this);
        websiteDialog.loadMenu(List.of(R.string.copy, R.string.share, R.string.open_in_browser, R.string.cookie),
                List.of(R.drawable.copy, R.drawable.share, R.drawable.export, R.drawable.cookie),
                List.of(_ -> params.copy("url:", web.getUrl()),
                        _ -> startActivity(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, trim(web.getUrl()))),
                        _ -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(trim(web.getUrl())))),
                        _ -> {
                            String targetUrl = trim(web.getUrl());
                            cookieDialog.setValue(cookie.getCookie(targetUrl));
                            cookieDialog.getDialog().setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.save), (_, _) -> cookie.setCookie(targetUrl, cookieDialog.getText()));
                            cookieDialog.getDialog().setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.clear), (_, _) -> cookie.setCookie(targetUrl, ""));
                            cookieDialog.show();
                        }), Integer.class);

        binding.js.setOnClickListener(_ -> {
            jsAdapter.set(js.searchJS(trim(web.getUrl())));
            jsDialog.show();
        });
        binding.menu.setOnClickListener(_ -> menuDialog.show());
        binding.browser.setOnClickListener(_ -> browserDialog.show());
        binding.website.setOnClickListener(_ -> websiteDialog.show());


        progress.observe(this, p -> {
            refreshButton.setIconResource(p == 100 ? R.drawable.refresh : R.drawable.close);
            refreshButton.setText(p == 100 ? R.string.refresh : R.string.stop);
        });
        cookie = CookieManager.getInstance();
        binding.back.setOnClickListener(_ -> {
            if (web.canGoBack()) web.goBack();
        });
        binding.forward.setOnClickListener(_ -> {
            if (web.canGoForward()) web.goForward();
        });
        if (getIntent().hasExtra("data") && getIntent().getStringExtra("data") != null) {
            this.webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36");
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

    static class JSAdapter extends RecyclerAdapter<JSONObject> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preference, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemPreferenceBinding binding = ItemPreferenceBinding.bind(holder.itemView);
            JSONObject item = get(position);
            binding.itemTitle.setText(item.getString("title"));
            binding.itemContent.setText(item.getString("description"));
            binding.itemIcon.setImageResource(R.drawable.js);
            binding.getRoot().updateAppearance(position, getItemCount());
            super.onBindViewHolder(holder, position);
        }
    }
}