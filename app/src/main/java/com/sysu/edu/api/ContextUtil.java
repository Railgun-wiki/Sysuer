package com.sysu.edu.api;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.sysu.edu.R;
import com.sysu.edu.view.EditTextDialog;

import java.util.concurrent.ExecutionException;

public class ContextUtil {
    private final Context context;
    private final SharedPreferences sharedPreferences;

    public ContextUtil(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("privacy", Context.MODE_PRIVATE);
    }

    public Context getContext() {
        return context;
    }

    public int getColorFromAttr(int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    /**
     * 将 dp 值转换为 px 值
     *
     * @param dps dp 值
     * @return 对应的 px 值
     */
    public int dpToPx(int dps) {
        return Math.round(context.getResources().getDisplayMetrics().density * dps);
    }

    /**
     * 获取 Cookie
     *
     * @return Cookie
     */
    public String getCookie() {
        return sharedPreferences.getString("Cookie", "");
    }

    /**
     * 获取 Authorization
     *
     * @return Authorization
     */
    public String getAuthorization() {
        return sharedPreferences.getString("authorization", "");
    }

    public void setAuthorization(String auth) {
        sharedPreferences.edit().putString("authorization", auth).apply();
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUserName() {
        return sharedPreferences.getString("username", "");
    }

    /**
     * 设置用户名
     *
     * @param userName 用户名
     */
    public void setUserName(String userName) {
        sharedPreferences.edit().putString("username", userName).apply();
    }

    /**
     * 获取密码
     *
     * @return 密码
     */
    public String getPassword() {
        return sharedPreferences.getString("password", "");
    }

    /**
     * 设置密码
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        sharedPreferences.edit().putString("password", password).apply();
    }

    /**
     * 获取 SharedPreferences 对象
     *
     * @return SharedPreferences 对象
     */
    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    /**
     * 获取 Token
     *
     * @return Token
     */
    public String getToken() {
        return sharedPreferences.getString("token", "");
    }

    /**
     * 设置 Token
     *
     * @param token Token
     */
    public void setToken(String token) {
        sharedPreferences.edit().putString("token", token).apply();
    }

    /**
     * 获取是否为开发者
     *
     * @return 是否为开发者
     */
    public boolean isDeveloper() {
        return sharedPreferences.getBoolean("developer", false);
    }

    /**
     * 设置是否为开发者
     *
     * @param developer 是否为开发者
     */
    public void setDeveloper(boolean developer) {
        sharedPreferences.edit().putBoolean("developer", developer).apply();
    }

    /**
     * 复制文本到剪贴板
     *
     * @param tag  剪贴板标签
     * @param text 要复制的文本
     */
    public void copy(String tag, String text) {
        ClipboardManager clip = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setPrimaryClip(ClipData.newPlainText(tag, text));
    }

    /**
     * 显示 Toast 消息
     *
     * @param resource 字符串资源 ID
     */
    public void toast(int resource) {
        Toast.makeText(context, resource, Toast.LENGTH_LONG).show();
    }

    /**
     * 显示 Toast 消息
     *
     * @param toast 要显示的文本
     */
    public void toast(String toast) {
        Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
    }

    /**
     * 获取登录模式
     *
     * @return 登录模式（"0"：弹窗登录；"1"：主页弹窗、其他跳转登录；"2"：跳转登录；"3"：自动登录）
     */
    public String getLoginMode() {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("loginMode", "2");
    }

    /**
     * 登录
     *
     * @param url        登录 URL,建议使用 TargeterURL 中的默认登录 URL
     * @param afterLogin 登录成功后的回调 Runnable 对象
     *
     */

    public void login(String url, Runnable afterLogin) {
        if (getUserName().isEmpty()) {
            EditTextDialog username = new EditTextDialog(context);
            username.setHint(R.string.username);
            username.setTitle(R.string.username);
            username.getDialog().setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.confirm), (_, _) -> {
                setUserName(username.getText());
                System.out.println("Login with " + username.getText());
                login(url, afterLogin);
            });
            username.show();
            return;
        }
        if (getPassword().isEmpty()) {
            EditTextDialog password = new EditTextDialog(context);
            password.setHint(R.string.password);
            password.setTitle(R.string.password);
            password.setPasswordMode();
            password.getDialog().setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.confirm), (_, _) -> {
                setPassword(password.getText());
                password.getDialog().dismiss();
                login(url, afterLogin);
            });
            password.show();
            return;
        }
        LoginManager loginManager = new LoginManager();
        loginManager.setAuthorization(new AuthorizationJar(context));
        boolean login = false;
        try {
            login = loginManager.login(getUserName(), getPassword(), url);
        } catch (ExecutionException | InterruptedException e) {
            Log.e("ContextUtil", "login: ", e);
        }
        System.out.println("Login result: " + login);
        if (login && afterLogin != null) afterLogin.run();
    }
}
