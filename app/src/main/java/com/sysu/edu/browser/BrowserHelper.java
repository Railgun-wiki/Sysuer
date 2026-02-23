package com.sysu.edu.browser;

import static com.sysu.edu.api.FileManager.readAssets;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class BrowserHelper extends SQLiteOpenHelper {
    private final Context context;

    public BrowserHelper(Context context) {
        super(context, "browser.db", null, 3);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS ua (id INTEGER PRIMARY KEY AUTOINCREMENT, uaId INTEGER UNIQUE, position INTEGER, title TEXT, ua TEXT, description TEXT, time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE IF NOT EXISTS js (id INTEGER PRIMARY KEY AUTOINCREMENT, jsId INTEGER UNIQUE, position INTEGER, title TEXT, script TEXT, description TEXT, matches TEXT, state INTEGER DEFAULT 1, run INTEGER DEFAULT 0,author TEXT, time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        JSONArray js = JSONArray.parseArray(readAssets(getContext(), "js.json"));
        JSONArray ua = JSONArray.parse(readAssets(getContext(), "ua.json"));
        js.forEach((i) -> {
            JSONObject item = (JSONObject) i;
            db.execSQL("INSERT OR IGNORE INTO js (jsId, position, title, script, description, matches) VALUES (?, ?, ?, ?, ?, ?)", new Object[]{item.getInteger("jsId"), item.getInteger("position"), item.getString("title"), item.getString("script"), item.getString("description"), item.getJSONArray("matches").toJSONString()});
        });
        ua.forEach((i) -> {
            JSONObject item = (JSONObject) i;
            db.execSQL("INSERT OR IGNORE INTO ua (uaId, position, title, ua, description) VALUES (?, ?, ?, ?, ?)", new Object[]{item.getInteger("uaId"), item.getInteger("position"), item.getString("title"), item.getString("ua"), item.getString("description")});
        });
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE js ADD COLUMN state INTEGER DEFAULT 1");
            ContentValues state = new ContentValues();
            state.put("state", 1);
            db.update("js", state, null, null);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE js ADD COLUMN run INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE js ADD COLUMN author TEXT");
        }
    }

    public Context getContext() {
        return context;
    }

}
